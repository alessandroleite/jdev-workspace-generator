package alessandro;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Pom {

	private String groupId;
	private String artifactId;
	private String version;
	
	private final Map<String, String> profileProperties = new HashMap<String, String>();

	public Pom() {
		super();
	}

	public Pom(String groupId, String artifactId, String version) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	
	public String addProfileProperties(String property, String value){
		return this.profileProperties.put(property, value);
	}

	public Map<String, String> getProfileProperties() {
		return Collections.unmodifiableMap(this.profileProperties);
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "groupId=" + this.getGroupId() + ",artifactId="
				+ this.getArtifactId() + ",version=" + this.getVersion();
	}
}