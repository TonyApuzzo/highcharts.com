package com.highcharts.export.pool;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;

import com.highcharts.export.server.Server;
import com.highcharts.export.server.ServerState;

public class ServerObjectFactory implements ObjectFactory<Server> {

	public String exec;
	public String script;
	private String host;
	private int basePort;
	private int readTimeout;
	private int connectTimeout;
	private int maxTimeout;
	private static HashMap<Integer,PortStatus> portUsage = new HashMap<Integer, PortStatus>();
	protected static Logger logger = Logger.getLogger("pool");

	private enum PortStatus {
        BUSY,
        FREE;
	}


	public ServerObjectFactory() {}

	@Override
	public synchronized Server makeObject() {
		logger.debug("in makeObject, " + exec + ", " +  script + ", " +  host);
		Integer port = this.getAvailablePort();
		portUsage.put(port, PortStatus.BUSY);
		return new Server(exec, script, host, port, connectTimeout, readTimeout, maxTimeout);
	}

	@Override
	public synchronized void removeObject(final Server server) {
		logger.debug("in destroyObject");
		ServerObjectFactory.releasePort(server.getPort());
		server.cleanup();
	}

	@Override
	public boolean validateObject(final Server server) {
		boolean isValid = false;
		try {
			if(server.getState() != ServerState.IDLE) {
				logger.debug("server didn\'t pass validation");
				return false;
			}

			String result = server.request("{\"status\":\"isok\"}");
			if(result.indexOf("OK") > -1) {
				isValid = true;
				logger.debug("server passed validation");
			} else {
				logger.debug("server didn\'t pass validation");
			}
		} catch (Exception e) {
			logger.error("Error while validating object in Pool: " + e.getMessage());
		}
		return isValid;
	}

	public static void releasePort(final Integer port) {
		logger.debug("Releasing port " + port);
		portUsage.put(port, PortStatus.FREE);
	}

	public Integer getAvailablePort() {
		for (Map.Entry<Integer, PortStatus> entry : portUsage.entrySet()) {
		   if (PortStatus.FREE == entry.getValue()) {
			   // return available port
			   logger.debug("Portusage " + portUsage.toString());
			   return entry.getKey();
		   }
		}
		// if no port is free
		logger.debug("Nothing free in Portusage " + portUsage.toString());
		return basePort + portUsage.size();
	}

	/*Getters and Setters*/

	public String getExec() {
		return exec;
	}

	public void setExec(final String exec) {
		this.exec = exec;
	}

	public String getScript() {
		return script;
	}

	public void setScript(final String script) {
		this.script = script;
	}

	public String getHost() {
		return host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public int getBasePort() {
		return basePort;
	}

	public void setBasePort(final int basePort) {
		this.basePort = basePort;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(final int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(final int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getMaxTimeout() {
		return maxTimeout;
	}

	public void setMaxTimeout(final int maxTimeout) {
		this.maxTimeout = maxTimeout;
	}

	@PostConstruct
	public void afterBeanInit() throws FileNotFoundException {
        final String defaultScriptResource = "/../phantomjs/highcharts-convert.js";
	    try {
	        if((script == null) || script.trim().isEmpty()) {
	            ClassLoader classLoader = getClass().getClassLoader();
	            final URL url = classLoader.getResource(defaultScriptResource);
	            if (null == url) {
	                throw new FileNotFoundException("Unable to find default script resource: " + defaultScriptResource);
	            }
	            final URI uri = new URI(url.toString());
	            final File file = new File(uri);
	            if (!file.canRead()) {
	                throw new FileNotFoundException("Unable to find default script: " + uri);
	            }
	            this.setScript(file.getAbsolutePath());
	        } else {
                final File file = new File(script);
                if (!file.canRead()) {
                    throw new FileNotFoundException("Unable to find script: " + script);
                }
	        }
	    } catch (URISyntaxException e) {
	        // This really should never happen, but just in case
	        FileNotFoundException fileNotFoundException = new FileNotFoundException(e.getMessage() + ": " + defaultScriptResource);
	        fileNotFoundException.initCause(e);
	        throw fileNotFoundException;
	    }
	}

}
