package com.aspectsecurity.automationservices.appscansource.utilities;

import java.io.IOException;

import javax.ws.rs.client.Invocation;

import com.aspectsecurity.automationservices.plugins.jenkins.appscansource.AppScanSourceCommand;
import com.aspectsecurity.automationservices.plugins.jenkins.appscansource.AppScanSourceInstallation;
import com.aspectsecurity.automationservices.plugins.jenkins.appscansource.AppScanSourceInvocation;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;

public class AppScanSourceExecutor {

	public static void execute(Run<?, ?> run, FilePath ws, Launcher launcher, String installation, Node node, TaskListener listener, EnvVars envVars, String cliScriptContent) throws IOException, InterruptedException{
		String exe = AppScanSourceInstallation.getExecutable(installation, AppScanSourceCommand.AppScanSourceCLI, node, listener, envVars);
		
		//Create a temp file with our script commands
		FilePath tempFile = ws.createTextTempFile("temp_cli_script_", ".txt", cliScriptContent);
		
		//Build the command line commands with necessary parameters
		JenkinsExecutor runner = new JenkinsExecutor(run, ws, launcher, listener);
		AppScanSourceInvocation invocation = new AppScanSourceInvocation(exe, run, ws, listener); 
		invocation.addScriptFile(tempFile);

		//Execute command line
		if (!invocation.execute(runner)) {
            throw new AbortException("AppScan Source execution failed");
        }			
		//Delete the temp file after we use it
		tempFile.delete();
	}
}
