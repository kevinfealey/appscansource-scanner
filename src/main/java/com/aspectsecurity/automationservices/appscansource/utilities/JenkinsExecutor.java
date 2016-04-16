package com.aspectsecurity.automationservices.appscansource.utilities;

import java.io.IOException;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

public class JenkinsExecutor
{
    private final Launcher launcher;
    private final Run<?, ?> build;
    private final TaskListener taskListener;
    private final FilePath workspace;

    public JenkinsExecutor(AbstractBuild<?, ?> build, Launcher launcher, BuildListener taskListener) {
        this.launcher = launcher;
        this.build = build;
        this.taskListener = taskListener;
        this.workspace = build.getWorkspace();
    }

    public JenkinsExecutor(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener taskListener) {
        this.launcher = launcher;
        this.build = build;
        this.taskListener = taskListener;
        this.workspace = workspace;
    }

    public boolean execute(ArgumentListBuilder args, Map<String, String> environment)
            throws IOException, InterruptedException
    {
        return launcher.launch()
                .pwd(workspace)
                .envs(environment)
                .cmds(args)
                .stdout(taskListener).join() == 0;
    }
}
