package io.github.hectorvent.floci.services.eks;

import io.github.hectorvent.floci.config.EmulatorConfig;
import io.github.hectorvent.floci.core.common.AwsException;
import io.github.hectorvent.floci.core.common.RegionResolver;
import io.github.hectorvent.floci.core.storage.InMemoryStorage;
import io.github.hectorvent.floci.core.storage.StorageFactory;
import io.github.hectorvent.floci.services.eks.model.ClusterStatus;
import io.github.hectorvent.floci.services.eks.model.CreateClusterRequest;
import io.github.hectorvent.floci.services.eks.model.CreateFargateProfileRequest;
import io.github.hectorvent.floci.services.eks.model.CreateNodeGroupRequest;
import io.github.hectorvent.floci.services.eks.model.FargateProfile;
import io.github.hectorvent.floci.services.eks.model.FargateProfileStatus;
import io.github.hectorvent.floci.services.eks.model.Cluster;
import io.github.hectorvent.floci.services.eks.model.NodeGroup;
import io.github.hectorvent.floci.services.eks.model.NodeGroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class EksServiceTest {

    private EksService eksService;
    private EmulatorConfig config;
    private EksClusterManager clusterManager;

    @BeforeEach
    void setUp() {
        StorageFactory storageFactory = Mockito.mock(StorageFactory.class);
        when(storageFactory.create(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(new InMemoryStorage<>(), new InMemoryStorage<>(), new InMemoryStorage<>());

        config = Mockito.mock(EmulatorConfig.class);
        var servicesConfig = Mockito.mock(EmulatorConfig.ServicesConfig.class);
        var eksConfig = Mockito.mock(EmulatorConfig.EksServiceConfig.class);

        when(config.services()).thenReturn(servicesConfig);
        when(servicesConfig.eks()).thenReturn(eksConfig);
        when(eksConfig.mock()).thenReturn(true);
        when(eksConfig.apiServerBasePort()).thenReturn(6500);
        when(config.defaultRegion()).thenReturn("us-east-1");

        clusterManager = Mockito.mock(EksClusterManager.class);
        RegionResolver regionResolver = new RegionResolver("us-east-1", "000000000000");
        eksService = new EksService(storageFactory, config, regionResolver, clusterManager);
    }

    @Test
    void createCluster() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("test-cluster");
        req.setRoleArn("arn:aws:iam::000000000000:role/eks-role");
        req.setVersion("1.29");

        Cluster cluster = eksService.createCluster(req);

        assertNotNull(cluster);
        assertEquals("test-cluster", cluster.getName());
        assertEquals(ClusterStatus.ACTIVE, cluster.getStatus());
        assertTrue(cluster.getArn().contains("test-cluster"));
        assertEquals("1.29", cluster.getVersion());
        assertNotNull(cluster.getCreatedAt());
    }

    @Test
    void createClusterDuplicateFails() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("dup-cluster");
        req.setRoleArn("arn:aws:iam::000000000000:role/eks-role");

        eksService.createCluster(req);

        assertThrows(AwsException.class, () -> eksService.createCluster(req));
    }

    @Test
    void describeCluster() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("my-cluster");
        req.setRoleArn("arn:aws:iam::000000000000:role/eks-role");
        eksService.createCluster(req);

        Cluster described = eksService.describeCluster("my-cluster");
        assertEquals("my-cluster", described.getName());
    }

    @Test
    void describeClusterNotFound() {
        AwsException ex = assertThrows(AwsException.class,
                () -> eksService.describeCluster("nonexistent"));
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void listClusters() {
        CreateClusterRequest req1 = new CreateClusterRequest();
        req1.setName("cluster-a");
        req1.setRoleArn("arn:aws:iam::000000000000:role/eks-role");

        CreateClusterRequest req2 = new CreateClusterRequest();
        req2.setName("cluster-b");
        req2.setRoleArn("arn:aws:iam::000000000000:role/eks-role");

        eksService.createCluster(req1);
        eksService.createCluster(req2);

        List<String> names = eksService.listClusters();
        assertEquals(2, names.size());
        assertTrue(names.contains("cluster-a"));
        assertTrue(names.contains("cluster-b"));
    }

    @Test
    void deleteCluster() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("to-delete");
        req.setRoleArn("arn:aws:iam::000000000000:role/eks-role");
        eksService.createCluster(req);

        Cluster deleted = eksService.deleteCluster("to-delete");
        assertEquals(ClusterStatus.DELETING, deleted.getStatus());
        assertTrue(eksService.listClusters().isEmpty());
    }

    @Test
    void taggingOperations() {
        CreateClusterRequest req = new CreateClusterRequest();
        req.setName("tagged-cluster");
        req.setRoleArn("arn:aws:iam::000000000000:role/eks-role");
        Cluster cluster = eksService.createCluster(req);

        String arn = cluster.getArn();

        // tagResource
        eksService.tagResource(arn, Map.of("env", "test", "team", "platform"));
        Map<String, String> tags = eksService.listTagsForResource(arn);
        assertEquals("test", tags.get("env"));
        assertEquals("platform", tags.get("team"));

        // untagResource
        eksService.untagResource(arn, List.of("env"));
        tags = eksService.listTagsForResource(arn);
        assertFalse(tags.containsKey("env"));
        assertEquals("platform", tags.get("team"));
    }

    @Test
    void createNodeGroupIncludesAwsShapeFields() {
        CreateClusterRequest clusterRequest = new CreateClusterRequest();
        clusterRequest.setName("my-eks-cluster");
        clusterRequest.setRoleArn("arn:aws:iam::000000000000:role/eks-role");
        eksService.createCluster(clusterRequest);

        NodeGroup.ScalingConfig scalingConfig = new NodeGroup.ScalingConfig();
        scalingConfig.setMinSize(1);
        scalingConfig.setMaxSize(3);
        scalingConfig.setDesiredSize(1);

        CreateNodeGroupRequest nodeGroupRequest = new CreateNodeGroupRequest();
        nodeGroupRequest.setNodegroupName("my-eks-nodegroup");
        nodeGroupRequest.setNodeRole("arn:aws:iam::000000000000:role/role-name");
        nodeGroupRequest.setVersion("1.26");
        nodeGroupRequest.setReleaseVersion("1.26.12-20240329");
        nodeGroupRequest.setScalingConfig(scalingConfig);
        nodeGroupRequest.setSubnets(List.of("subnet-0e2907431c9988b72", "subnet-04ad87f71c6e5ab4d"));
        nodeGroupRequest.setInstanceTypes(List.of("t3.medium"));

        NodeGroup nodeGroup = eksService.createNodeGroup("my-eks-cluster", nodeGroupRequest);

        assertEquals("my-eks-nodegroup", nodeGroup.getNodegroupName());
        assertTrue(nodeGroup.getNodegroupArn().contains("nodegroup/my-eks-cluster/my-eks-nodegroup"));
        assertEquals("my-eks-cluster", nodeGroup.getClusterName());
        assertEquals(NodeGroupStatus.CREATING, nodeGroup.getStatus());
        assertEquals("ON_DEMAND", nodeGroup.getCapacityType());
        assertEquals(3, nodeGroup.getScalingConfig().getMaxSize());
        assertEquals(List.of("t3.medium"), nodeGroup.getInstanceTypes());
        assertEquals("AL2_x86_64", nodeGroup.getAmiType());
        assertEquals("arn:aws:iam::000000000000:role/role-name", nodeGroup.getNodeRole());
        assertEquals(20, nodeGroup.getDiskSize());
        assertTrue(nodeGroup.getHealth().getIssues().isEmpty());
        assertEquals(1, nodeGroup.getUpdateConfig().getMaxUnavailable());
        assertEquals("my-eks-nodegroup", eksService.listNodeGroups("my-eks-cluster").getFirst());
    }

    @Test
    void createFargateProfileIncludesAwsShapeFields() {
        CreateClusterRequest clusterRequest = new CreateClusterRequest();
        clusterRequest.setName("my-eks-cluster");
        clusterRequest.setRoleArn("arn:aws:iam::000000000000:role/eks-role");
        eksService.createCluster(clusterRequest);

        FargateProfile.Selector selector = new FargateProfile.Selector();
        selector.setNamespace("default");
        selector.setLabels(Map.of("app", "api"));

        CreateFargateProfileRequest profileRequest = new CreateFargateProfileRequest();
        profileRequest.setFargateProfileName("my-fargate-profile");
        profileRequest.setPodExecutionRoleArn("arn:aws:iam::000000000000:role/eks-fargate-role");
        profileRequest.setSubnets(List.of("subnet-0e2907431c9988b72", "subnet-04ad87f71c6e5ab4d"));
        profileRequest.setSelectors(List.of(selector));
        profileRequest.setTags(Map.of("env", "test"));

        FargateProfile profile = eksService.createFargateProfile("my-eks-cluster", profileRequest);

        assertEquals("my-fargate-profile", profile.getFargateProfileName());
        assertTrue(profile.getFargateProfileArn().contains("fargateprofile/my-eks-cluster/my-fargate-profile"));
        assertEquals("my-eks-cluster", profile.getClusterName());
        assertEquals(FargateProfileStatus.CREATING, profile.getStatus());
        assertEquals("arn:aws:iam::000000000000:role/eks-fargate-role", profile.getPodExecutionRoleArn());
        assertEquals(List.of("subnet-0e2907431c9988b72", "subnet-04ad87f71c6e5ab4d"), profile.getSubnets());
        assertEquals("default", profile.getSelectors().getFirst().getNamespace());
        assertEquals("api", profile.getSelectors().getFirst().getLabels().get("app"));
        assertTrue(profile.getHealth().getIssues().isEmpty());
        assertEquals("test", profile.getTags().get("env"));
        assertEquals("my-fargate-profile", eksService.listFargateProfiles("my-eks-cluster").getFirst());
    }
}
