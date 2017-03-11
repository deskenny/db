
package db;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class StopResults {

	@SerializedName("errorcode")
	@Expose
	private String errorcode;
	@SerializedName("errormessage")
	@Expose
	private String errormessage;
	@SerializedName("numberofresults")
	@Expose
	private Integer numberofresults;
	@SerializedName("stopid")
	@Expose
	private String stopid;
	@SerializedName("timestamp")
	@Expose
	private String timestamp;
	@SerializedName("results")
	@Expose
	private List<Result> results = null;

	public String getErrorcode() {
		return errorcode;
	}

	public void setErrorcode(String errorcode) {
		this.errorcode = errorcode;
	}

	public String getErrormessage() {
		return errormessage;
	}

	public void setErrormessage(String errormessage) {
		this.errormessage = errormessage;
	}

	public Integer getNumberofresults() {
		return numberofresults;
	}

	public void setNumberofresults(Integer numberofresults) {
		this.numberofresults = numberofresults;
	}

	public String getStopid() {
		return stopid;
	}

	public void setStopid(String stopid) {
		this.stopid = stopid;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public List<Result> getResults() {
		return results;
	}

	public void setResults(List<Result> results) {
		this.results = results;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(errorcode).append(errormessage).append(numberofresults).append(stopid)
				.append(timestamp).append(results).toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof StopResults) == false) {
			return false;
		}
		StopResults rhs = ((StopResults) other);
		return new EqualsBuilder().append(errorcode, rhs.errorcode).append(errormessage, rhs.errormessage)
				.append(numberofresults, rhs.numberofresults).append(stopid, rhs.stopid)
				.append(timestamp, rhs.timestamp).append(results, rhs.results).isEquals();
	}

}
