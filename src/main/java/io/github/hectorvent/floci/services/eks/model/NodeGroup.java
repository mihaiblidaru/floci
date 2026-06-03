package io.github.hectorvent.floci.services.eks.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeGroup {

    @JsonProperty("nodegroupName")
    private String nodegroupName;

    @JsonProperty("nodegroupArn")
    private String nodegroupArn;

    @JsonProperty("clusterName")
    private String clusterName;

    @JsonProperty("version")
    private String version;

    @JsonProperty("releaseVersion")
    private String releaseVersion;

    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant createdAt;

    @JsonProperty("modifiedAt")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant modifiedAt;

    @JsonProperty("status")
    private NodeGroupStatus status;

    @JsonProperty("capacityType")
    private String capacityType;

    @JsonProperty("scalingConfig")
    private ScalingConfig scalingConfig;

    @JsonProperty("instanceTypes")
    private List<String> instanceTypes;

    @JsonProperty("subnets")
    private List<String> subnets;

    @JsonProperty("amiType")
    private String amiType;

    @JsonProperty("nodeRole")
    private String nodeRole;

    @JsonProperty("diskSize")
    private Integer diskSize;

    @JsonProperty("health")
    private Health health;

    @JsonProperty("updateConfig")
    private UpdateConfig updateConfig;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonIgnore
    private String accountId;

    public NodeGroup() {}

    public String getNodegroupName() { return nodegroupName; }
    public void setNodegroupName(String nodegroupName) { this.nodegroupName = nodegroupName; }

    public String getNodegroupArn() { return nodegroupArn; }
    public void setNodegroupArn(String nodegroupArn) { this.nodegroupArn = nodegroupArn; }

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public NodeGroupStatus getStatus() { return status; }
    public void setStatus(NodeGroupStatus status) { this.status = status; }

    public String getCapacityType() { return capacityType; }
    public void setCapacityType(String capacityType) { this.capacityType = capacityType; }

    public ScalingConfig getScalingConfig() { return scalingConfig; }
    public void setScalingConfig(ScalingConfig scalingConfig) { this.scalingConfig = scalingConfig; }

    public List<String> getInstanceTypes() { return instanceTypes; }
    public void setInstanceTypes(List<String> instanceTypes) { this.instanceTypes = instanceTypes; }

    public List<String> getSubnets() { return subnets; }
    public void setSubnets(List<String> subnets) { this.subnets = subnets; }

    public String getAmiType() { return amiType; }
    public void setAmiType(String amiType) { this.amiType = amiType; }

    public String getNodeRole() { return nodeRole; }
    public void setNodeRole(String nodeRole) { this.nodeRole = nodeRole; }

    public Integer getDiskSize() { return diskSize; }
    public void setDiskSize(Integer diskSize) { this.diskSize = diskSize; }

    public Health getHealth() { return health; }
    public void setHealth(Health health) { this.health = health; }

    public UpdateConfig getUpdateConfig() { return updateConfig; }
    public void setUpdateConfig(UpdateConfig updateConfig) { this.updateConfig = updateConfig; }

    public Map<String, String> getLabels() { return labels; }
    public void setLabels(Map<String, String> labels) { this.labels = labels; }

    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScalingConfig {
        @JsonProperty("minSize")
        private Integer minSize;

        @JsonProperty("maxSize")
        private Integer maxSize;

        @JsonProperty("desiredSize")
        private Integer desiredSize;

        public ScalingConfig() {}

        public Integer getMinSize() { return minSize; }
        public void setMinSize(Integer minSize) { this.minSize = minSize; }

        public Integer getMaxSize() { return maxSize; }
        public void setMaxSize(Integer maxSize) { this.maxSize = maxSize; }

        public Integer getDesiredSize() { return desiredSize; }
        public void setDesiredSize(Integer desiredSize) { this.desiredSize = desiredSize; }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateConfig {
        @JsonProperty("maxUnavailable")
        private Integer maxUnavailable;

        @JsonProperty("maxUnavailablePercentage")
        private Integer maxUnavailablePercentage;

        public UpdateConfig() {}

        public Integer getMaxUnavailable() { return maxUnavailable; }
        public void setMaxUnavailable(Integer maxUnavailable) { this.maxUnavailable = maxUnavailable; }

        public Integer getMaxUnavailablePercentage() { return maxUnavailablePercentage; }
        public void setMaxUnavailablePercentage(Integer maxUnavailablePercentage) {
            this.maxUnavailablePercentage = maxUnavailablePercentage;
        }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Health {
        @JsonProperty("issues")
        private List<Issue> issues;

        public Health() {}

        public List<Issue> getIssues() { return issues; }
        public void setIssues(List<Issue> issues) { this.issues = issues; }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("resourceIds")
        private List<String> resourceIds;

        public Issue() {}

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getResourceIds() { return resourceIds; }
        public void setResourceIds(List<String> resourceIds) { this.resourceIds = resourceIds; }
    }
}
