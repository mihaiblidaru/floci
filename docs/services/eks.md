# EKS (Elastic Kubernetes Service)

**Protocol:** REST-JSON  
**Endpoint:** `http://localhost:4566/` (path-routed via JAX-RS)

EKS uses a standard REST API with JSON bodies — not the JSON 1.1 (`X-Amz-Target`) or Query protocol.

## Supported Operations

| Operation | Description |
|---|---|
| `CreateCluster` | Create a new EKS cluster |
| `DescribeCluster` | Describe a cluster by name |
| `ListClusters` | List all cluster names |
| `DeleteCluster` | Delete a cluster |
| `CreateNodegroup` | Create node group metadata for a cluster |
| `DescribeNodegroup` | Describe a node group by cluster and name |
| `ListNodegroups` | List node group names for a cluster |
| `DeleteNodegroup` | Delete a node group |
| `CreateFargateProfile` | Create Fargate profile metadata for a cluster |
| `DescribeFargateProfile` | Describe a Fargate profile by cluster and name |
| `ListFargateProfiles` | List Fargate profile names for a cluster |
| `DeleteFargateProfile` | Delete a Fargate profile |
| `TagResource` | Add tags to a cluster |
| `UntagResource` | Remove tags from a cluster |
| `ListTagsForResource` | List tags on a cluster |

## Modes

### Mock mode (`mock: true`)

Cluster metadata is stored in-process. No Docker containers are started. The cluster transitions directly to `ACTIVE` on creation. Use this in CI or whenever you only need the EKS API shape, not a real Kubernetes API server.

### Real mode (`mock: false`, default)

Floci starts a **k3s** (`rancher/k3s`) container for each cluster. The k3s API server is exposed on a host port from the configured range (`6500–6599`). Once `/readyz` responds, the cluster transitions to `ACTIVE` and the CA certificate is extracted from the kubeconfig.

!!! note "Docker socket required"
    Real mode starts privileged Docker containers. Mount the Docker socket and set the Docker network so containers can reach each other.

```yaml
services:
  floci:
    image: floci/floci:latest
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    ports:
      - "4566:4566"
    environment:
      FLOCI_SERVICES_EKS_DOCKER_NETWORK: my_project_default
```

!!! note "No port mapping needed for k3s ports"
    k3s containers bind their API server port (6500–6599) directly on the host via Docker — no `ports:` entry is required in `docker-compose.yml`. See [Ports Reference](../configuration/ports.md#ports-65006599-eks-real-mode) for the full explanation.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `FLOCI_SERVICES_EKS_ENABLED` | `true` | Enable the EKS service |
| `FLOCI_SERVICES_EKS_MOCK` | `false` | Metadata-only mode (no Docker) |
| `FLOCI_SERVICES_EKS_DEFAULT_IMAGE` | `rancher/k3s:latest` | k3s Docker image |
| `FLOCI_SERVICES_EKS_API_SERVER_BASE_PORT` | `6500` | First port in the k3s API server range |
| `FLOCI_SERVICES_EKS_API_SERVER_MAX_PORT` | `6599` | Last port in the k3s API server range |
| `FLOCI_SERVICES_EKS_DATA_PATH` | `./data/eks` | Host bind-mount root for cluster data |
| `FLOCI_SERVICES_EKS_DOCKER_NETWORK` | *(unset)* | Docker network for k3s containers |
| `FLOCI_SERVICES_EKS_KEEP_RUNNING_ON_SHUTDOWN` | `false` | Leave k3s containers running after Floci stops |

### Mock mode (CI / tests)

Use `FLOCI_SERVICES_EKS_MOCK=true` when you only need the API shape:

```yaml
# docker-compose.yml — CI / test environment
services:
  floci:
    image: floci/floci:latest
    environment:
      FLOCI_SERVICES_EKS_MOCK: "true"
```

## ARN Format

```
arn:aws:eks:<region>:<accountId>:cluster/<clusterName>
```

Node groups use:

```
arn:aws:eks:<region>:<accountId>:nodegroup/<clusterName>/<nodegroupName>/<id>
```

Fargate profiles use:

```
arn:aws:eks:<region>:<accountId>:fargateprofile/<clusterName>/<fargateProfileName>
```

## Examples

```bash
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

# Create a cluster
aws eks create-cluster \
  --name my-cluster \
  --role-arn arn:aws:iam::000000000000:role/eks-role \
  --resources-vpc-config subnetIds=[],securityGroupIds=[] \
  --kubernetes-version 1.29

# Describe the cluster
aws eks describe-cluster --name my-cluster

# List clusters
aws eks list-clusters

# Create a node group
curl -s -X POST "$AWS_ENDPOINT_URL/clusters/my-cluster/nodegroups" \
  -H "Content-Type: application/json" \
  -d '{
    "nodegroupName": "my-nodegroup",
    "nodeRole": "arn:aws:iam::000000000000:role/eks-node-role",
    "subnets": ["subnet-123", "subnet-456"],
    "instanceTypes": ["t3.medium"],
    "scalingConfig": {
      "minSize": 1,
      "maxSize": 3,
      "desiredSize": 1
    }
  }'

# Describe the node group
curl -s "$AWS_ENDPOINT_URL/clusters/my-cluster/nodegroups/my-nodegroup"

# List node groups
curl -s "$AWS_ENDPOINT_URL/clusters/my-cluster/nodegroups"

# Delete the node group
curl -s -X DELETE "$AWS_ENDPOINT_URL/clusters/my-cluster/nodegroups/my-nodegroup"

# Create a Fargate profile
curl -s -X POST "$AWS_ENDPOINT_URL/clusters/my-cluster/fargate-profiles" \
  -H "Content-Type: application/json" \
  -d '{
    "fargateProfileName": "my-fargate-profile",
    "podExecutionRoleArn": "arn:aws:iam::000000000000:role/eks-fargate-role",
    "subnets": ["subnet-123", "subnet-456"],
    "selectors": [
      {
        "namespace": "default",
        "labels": {
          "app": "api"
        }
      }
    ],
    "tags": {
      "env": "dev"
    }
  }'

# Describe the Fargate profile
curl -s "$AWS_ENDPOINT_URL/clusters/my-cluster/fargate-profiles/my-fargate-profile"

# List Fargate profiles
curl -s "$AWS_ENDPOINT_URL/clusters/my-cluster/fargate-profiles"

# Delete the Fargate profile
curl -s -X DELETE "$AWS_ENDPOINT_URL/clusters/my-cluster/fargate-profiles/my-fargate-profile"

# Tag a cluster
aws eks tag-resource \
  --resource-arn arn:aws:eks:us-east-1:000000000000:cluster/my-cluster \
  --tags env=dev,team=platform

# Delete a cluster
aws eks delete-cluster --name my-cluster
```

## Java SDK Example

```java
EksClient eks = EksClient.builder()
    .endpointOverride(URI.create("http://localhost:4566"))
    .region(Region.US_EAST_1)
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("test", "test")))
    .build();

// Create cluster
CreateClusterResponse created = eks.createCluster(r -> r
    .name("my-cluster")
    .roleArn("arn:aws:iam::000000000000:role/eks-role")
    .resourcesVpcConfig(v -> v
        .subnetIds(List.of())
        .securityGroupIds(List.of()))
    .version("1.29")
    .tags(Map.of("env", "dev")));

// Describe cluster
DescribeClusterResponse described = eks.describeCluster(r -> r
    .name("my-cluster"));

System.out.println(described.cluster().status()); // ACTIVE

// List clusters
List<String> names = eks.listClusters(r -> {}).clusters();

// Node group and Fargate profile support are currently exposed through the REST paths.

// Tag resource
eks.tagResource(r -> r
    .resourceArn(created.cluster().arn())
    .tags(Map.of("team", "platform")));

// Delete cluster
eks.deleteCluster(r -> r.name("my-cluster"));
```

## Not Implemented (Phase 1)

The following EKS features are not yet supported:

- `UpdateClusterConfig` / `UpdateClusterVersion`
- Add-ons (`CreateAddon`, `DescribeAddon`, `ListAddons`)
- Identity provider configs
- Access entries and policies
- Encryption config
