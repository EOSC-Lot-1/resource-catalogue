package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.net.URL;
import java.util.List;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ToolSecurity {

	@XmlElement()
	@Schema
	@FieldValidation(containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.TOOL_SECURITY_STATUS)
	private String status;

	@XmlElement()
	@Schema
	@FieldValidation(containsId = true, idClass = Vocabulary.class)
	@VocabularyValidation(type = Vocabulary.Type.TOOL_VULNERABILITIES)
	private String vulnerabilities;

	@XmlElement()
	@Schema
	private String lastCheck;

	@XmlElement()
	@Schema
	private URL reportUrl;

	public ToolSecurity() {
	}

	public ToolSecurity(String status, String vulnerabilities, String lastCheck, URL reportUrl) {
		this.status = status;
		this.vulnerabilities = vulnerabilities;
		this.lastCheck = lastCheck;
		this.reportUrl = reportUrl;
	}

	@Override
	public String toString() {
		return "ToolSecurity{" + " status='" + status + '\'' + ", lastCheck='" + lastCheck + '\''
				+ ", vulnerabilities='" + vulnerabilities + '\'' + ", reportUrl='" + reportUrl + '\'' + '}';
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLastCheck() {
		return lastCheck;
	}

	public void setLastCheck(String lastCheck) {
		this.lastCheck = lastCheck;
	}
	
	public String getVulnerabilities() {
		return vulnerabilities;
	}

	public void setVulnerabilities(String vulnerabilities) {
		this.vulnerabilities = vulnerabilities;
	}

	public URL getReportUrl() {
		return reportUrl;
	}

	public void setReportUrl(URL reportUrl) {
		this.reportUrl = reportUrl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ToolSecurity that = (ToolSecurity) o;
		return Objects.equals(status, that.status) && Objects.equals(lastCheck, that.lastCheck)
				&& Objects.equals(vulnerabilities, that.vulnerabilities) && Objects.equals(reportUrl, that.reportUrl);
	}

	@Override
	public int hashCode() {
		return Objects.hash(status, lastCheck, vulnerabilities, reportUrl);
	}
}
