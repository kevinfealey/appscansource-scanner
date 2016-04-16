package com.aspectsecurity.automationservices.plugins.jenkins.appscansource;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.aspectsecurity.automationservices.appscansource.utilities.JenkinsExecutor;


public class AppScanSourceInvocation {
	
	private String exe, scriptFile;
	private final Map<String, String> environment = new HashMap<String, String>();
	
	public AppScanSourceInvocation(String exe, Run<?, ?> build, FilePath ws, TaskListener listener)
            throws IOException, InterruptedException {
		this.exe = exe;
		
	}
	
    public boolean execute(JenkinsExecutor runner) throws IOException, InterruptedException {
    	return runner.execute(buildCommandLine(), environment);
    }
    
    protected ArgumentListBuilder appendExecutable(ArgumentListBuilder args) {
        args.add(exe);
        return args;
    }
    
    public AppScanSourceInvocation addScriptFile(FilePath scriptFile) {
        this.scriptFile = scriptFile.getRemote();
        return this;
    }
    
    protected ArgumentListBuilder appendScript(ArgumentListBuilder args) {
    	if (StringUtils.isNotBlank(scriptFile)) {
            args.add("script");
            args.add(scriptFile);
        }
    	return args;
    }
    
    protected ArgumentListBuilder buildCommandLine() throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        appendExecutable(args);
        appendScript(args);

        return args;
    }
}
