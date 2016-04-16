package com.aspectsecurity.automationservices.plugins.jenkins.appscansource;

public enum AppScanSourceCommand {
	AppScanSourceCLI("bin\\AppScanSrcCli.exe"), AppScanSrcAuto("bin\\ounceauto.exe"), AppScanSrcExe("bin\\AppScanSrc.exe");

    private final String name;

    private AppScanSourceCommand(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
