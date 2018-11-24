package ca.concordia.httpClient.lib;

import java.util.HashMap;
import java.util.Map;

public abstract class ClientHttpRequest {

	
	private Enum<HttpMethod> method;
	
	private String URI;
	
	private String version = "HTTP/1.0";
	
	private String host;
	
	private int port = 8080;
	
	private Map<String, String> queries = new HashMap<String, String>();
	
	private Map<String, String> headers = new HashMap<String, String>();
	
	private boolean isVerbose;
	
	private boolean isPrintedToFile;
	
	private String outputFilePath;
	
	/**
	 * @return the outputFilePath
	 */
	public String getOutputFilePath() {
		return outputFilePath;
	}

	/**
	 * @param outputFilePath the outputFilePath to set
	 */
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	private boolean hasHeaders;
	/**
	 * @return the isPrintedToFile
	 */
	public boolean isPrintedToFile() {
		return isPrintedToFile;
	}

	/**
	 * @param isPrintedToFile the isPrintedToFile to set
	 */
	public void setPrintedToFile(boolean isPrintedToFile) {
		this.isPrintedToFile = isPrintedToFile;
	}

	
	
	public ClientHttpRequest() {
		//could set default
	}

	/**
	 * @return the method
	 */
	public Enum<HttpMethod> getMethod() {
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(Enum<HttpMethod> method) {
		this.method = method;
	}

	/**
	 * @return the uRI
	 */
	public String getURI() {
		return URI;
	}

	/**
	 * @param uRI the uRI to set
	 */
	public void setURI(String uRI) {
		URI = uRI;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the queries
	 */
	public Map<String, String> getQueries() {
		return queries;
	}

	/**
	 * @param queries the queries to set
	 */
	public void setQueries(Map<String, String> queries) {
		this.queries = queries;
	}

	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	/**
	 * @return the isVerbose
	 */
	public boolean isVerbose() {
		return isVerbose;
	}

	/**
	 * @param isVerbose the isVerbose to set
	 */
	public void setVerbose(boolean isVerbose) {
		this.isVerbose = isVerbose;
	}

	/**
	 * @return the hasHeaders
	 */
	public boolean isHasHeaders() {
		return hasHeaders;
	}

	/**
	 * @param hasHeaders the hasHeaders to set
	 */
	public void setHasHeaders(boolean hasHeaders) {
		this.hasHeaders = hasHeaders;
	};

}
