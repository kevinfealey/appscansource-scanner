package com.aspectsecurity.automationservices.plugins.jenkins.appscansource;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.aspectsecurity.automationservices.appscansource.utilities.AppScanSourceExecutor;
import com.aspectsecurity.automationservices.appscansource.utilities.JenkinsExecutor;

import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class AppScanSourceBuilder extends Builder implements SimpleBuildStep {

    private final boolean disableScan;
    private final boolean acceptSSL;
    private final String scanWorkspace;
    private final String applicationFile;
    private String installation;
    Jenkins j = Jenkins.getInstance();
    private final static String applicationFileNameValidationError="Please point to a PAF or SLN file";
    
    private PrintStream logger;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public AppScanSourceBuilder(String installation, boolean disableScan, String scanWorkspace, String applicationFile, boolean acceptSSL) {
    	this.disableScan=disableScan;
        this.scanWorkspace=scanWorkspace;
        this.applicationFile=applicationFile;
        this.installation=installation;
        this.acceptSSL=acceptSSL;
      
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getDisableScan() {
        return disableScan;
    }

    public boolean getAcceptSSL() {
        return acceptSSL;
    }
    
    public String getScanWorkspace() {
        return scanWorkspace;
    }
    
    public String getApplicationFile() {
        return applicationFile;
    }
    
    public String getInstallation() {
        return installation;
    }
    
    @DataBoundSetter
    public void setInstallation(String installation) {
        this.installation = installation;
    }
    
    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException{
    	Computer computer = Computer.currentComputer();
        if (computer == null) {
            throw new AbortException("The AppScan Source build step requires to be launched on a node");
        }
        perform(build, computer.getNode(), workspace, launcher, listener, build.getEnvironment(listener));
    }
    
    public void perform(@Nonnull Run<?, ?> run, @Nonnull Node node, @Nonnull FilePath ws, @Nonnull Launcher launcher, @Nonnull TaskListener listener, EnvVars envVars)
            throws InterruptedException, IOException {

    	logger = listener.getLogger();
    	
    	if(!this.disableScan){
    		//Check that the application file exists and has the expected extension
    		boolean applicationFileOk = false;
			if(!checkApplicationFileName(this.applicationFile)){
				logger.println(applicationFileNameValidationError);
			} else {
				if(!checkApplicationFileNameExists(this.applicationFile)){
					logger.println(this.applicationFile + " does not exist.");
				} else {
					applicationFileOk = true;
				}
			}
			//Check that we can access the scan workspace
			boolean scanWorkspaceOk = checkScanWorkspace(this.scanWorkspace);
			
			//Only run the scan if the workspace and application file are ok
			if(applicationFileOk && scanWorkspaceOk){
				logger.println("Scanning " + applicationFile + " with AppScan Source");
				
				//String exe = AppScanSourceInstallation.getExecutable(installation, AppScanSourceCommand.AppScanSourceCLI, node, listener, envVars);
				
				//Determine if we need to use acceptssl flag
				String acceptSSLValue="";
				if(acceptSSL){
					acceptSSLValue="-acceptssl";
				}
				
				//Build the script file we'll pass into the AppScan Source CLI
				String cliScriptContent = "login_file " + getDescriptor().getASE_URL() + " " + getDescriptor().getLoginTokenFilePath() + " " + acceptSSLValue + System.lineSeparator();
				cliScriptContent += "oa " + applicationFile + System.lineSeparator();
				cliScriptContent += "sc " + scanWorkspace + System.lineSeparator();
				
				AppScanSourceExecutor.execute(run, ws, launcher, installation, node, listener, envVars, cliScriptContent);
				/*
				//Create a temp file with our script commands
				FilePath tempFile = ws.createTextTempFile("temp_cli_script_", ".txt", cliScriptContent);
				
				//Build the command line commands with necessary parameters
				CLIRunner runner = new CLIRunner(run, ws, launcher, listener);
				AppScanSourceInvocation invocation = new AppScanSourceInvocation(exe, run, ws, listener); 
				invocation.addScriptFile(tempFile);
				
				//Execute command line
				if (!invocation.execute(runner)) {
	                throw new AbortException("AppScan Source execution failed");
	            }			
				//Delete the temp file after we use it
				tempFile.delete();
				*/
			} else {
				logger.println("Please resolve issues with your application file or scan workspace configuration.");
			}
		} else {
			logger.println("Scan disabled in configuration. Not running scan.");
		}
   
    }

    /*****************************************************************************
     * Descriptor Below
     * 
     ****************************************************************************/
    // Overridden for better type safety.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    //Field Validator Helpers
    public static boolean checkApplicationFileName(String filePath){
    	if ( filePath.endsWith(".paf") || filePath.endsWith(".sln") ){
    		return true;
    	}
    	
    	return false;
    }
    
    public static boolean checkApplicationFileNameExists(String filePath){
    	return new File(filePath).exists();
    }
    
    public boolean checkScanWorkspace(String jobScanWorkspace){
    	File path = new File(jobScanWorkspace);
    	if(!path.exists()){
			logger.println("Scan Workspace does not exist.");
			logger.println(path.getAbsolutePath());
			return false;
		}
    	return true;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
    	String loginTokenFilePath, ASE_URL, installation;
        Jenkins j = Jenkins.getInstance();

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }
        
        public ListBoxModel doFillInstallationItems() {
            ListBoxModel model = new ListBoxModel();
            for (AppScanSourceInstallation tool : AppScanSourceInstallation.allInstallations()) {
                model.add(Util.fixEmptyAndTrim(tool.getName()));
            }
            return model;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Run AppScan Source";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
        	//installation = formData.getString("installation");
        	loginTokenFilePath = formData.getString("loginTokenFilePath");
        	ASE_URL = formData.getString("ASE_URL");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

		public String getLoginTokenFilePath() {
			return loginTokenFilePath;
		}

		public String getASE_URL() {
			return ASE_URL;
		}

		public String getInstallation() {
			return installation;
		}
		
		//Field Validators
        public FormValidation doCheckApplicationFile(@QueryParameter String value)
                throws IOException, ServletException {
            	if(!AppScanSourceBuilder.checkApplicationFileName(value)){
            		return FormValidation.error(applicationFileNameValidationError);
            	}
         
            return FormValidation.ok();
        }
        
        public FormValidation doCheckInstallation(@QueryParameter String value){
        	if (AppScanSourceInstallation.allInstallations().length >= 0){
        		try{
        			if(AppScanSourceInstallation.allInstallations()[0].getName() != null){
        				return FormValidation.ok();
        			}
        		} catch (Exception e){
        			return FormValidation.errorWithMarkup("Please configure AppScan Source installations from <a href=\""+ j.getRootUrl() +"configure\" target=\"_new\">the system configuration.</a>");
        		}
        	}
        	return FormValidation.errorWithMarkup("Please configure AppScan Source installations from <a href=\""+ j.getRootUrl() +"configure\" target=\"_new\">the system configuration.</a>");
        }

        public FormValidation doCheckLoginTokenFilePath(@QueryParameter String value) throws IOException, InterruptedException{
        	FilePath tokenPath = new FilePath(new File(value));
        	if(!tokenPath.exists()){
        		return FormValidation.warning(tokenPath.getName() + "is not a directory on the Jenkins master (but perhaps it exists on some slaves)");
        	}
        	return FormValidation.ok();
        }
        
    }
}

