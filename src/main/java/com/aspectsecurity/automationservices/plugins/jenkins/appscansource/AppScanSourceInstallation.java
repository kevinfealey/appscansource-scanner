package com.aspectsecurity.automationservices.plugins.jenkins.appscansource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.tools.ToolProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;

public class AppScanSourceInstallation extends ToolInstallation {
	
	@DataBoundConstructor
	public AppScanSourceInstallation(String name, String home,
			List<? extends ToolProperty<?>> properties) {
		super(name, home, properties);
	}

	public AppScanSourceInstallation forEnvironment(EnvVars environment) {
        return new AppScanSourceInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public AppScanSourceInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new AppScanSourceInstallation(getName(), translateFor(node, log), getProperties().toList());
    }
    
    public static String getExecutable(String name, AppScanSourceCommand command, Node node, TaskListener listener, EnvVars env) throws IOException, InterruptedException {
        if (name != null) {
            Jenkins j = Jenkins.getInstance();
            if (j != null) {
                for (AppScanSourceInstallation tool : j.getDescriptorByType(DescriptorImpl.class).getInstallations()) {
                    if (tool.getName().equals(name)) {
                        if (node != null) {
                            tool = tool.forNode(node, listener);
                        }
                        if (env != null) {
                            tool = tool.forEnvironment(env);
                        }
                        String home = Util.fixEmpty(tool.getHome());
                        if (home != null) {
                            if (node != null) {
                                FilePath homePath = node.createPath(home);
                                if (homePath != null) {
                                    return homePath.child(command.getName()).getRemote();
                                }
                            }
                            return home + "/" + command.getName();
                        }
                    }
                }
            }
        }
        return command.getName();
    }

    public static AppScanSourceInstallation[] allInstallations() {
        AppScanSourceInstallation.DescriptorImpl AppScanSourceDescriptor = Jenkins.getInstance().getDescriptorByType(AppScanSourceInstallation.DescriptorImpl.class);
        return AppScanSourceDescriptor.getInstallations();
    }

    public static AppScanSourceInstallation getInstallation(String AppScanSourceInstallation) throws IOException {
        AppScanSourceInstallation[] installations = allInstallations();
        if (AppScanSourceInstallation == null) {
            if (installations.length == 0) {
                throw new IOException("AppScan Source not found");
            }
            return installations[0];
        } else {
            for (AppScanSourceInstallation installation: installations) {
                if (AppScanSourceInstallation.equals(installation.getName())) {
                    return installation;
                }
            }
        }
        throw new IOException("AppScan Source not found");
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<AppScanSourceInstallation> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            super.configure(req, json);
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return "AppScan Source";
        }
    }
}