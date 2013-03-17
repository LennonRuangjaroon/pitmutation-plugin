package org.jenkinsci.plugins.pitmutation;

import hudson.FilePath;
import hudson.model.Result;

import hudson.util.TextFile;
import org.jenkinsci.plugins.pitmutation.targets.ProjectMutations;
import org.kohsuke.stapler.StaplerProxy;

import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author edward
 */
public class PitBuildAction implements HealthReportingAction, StaplerProxy {

  public PitBuildAction(AbstractBuild<?,?> owner) {
    owner_ = owner;
  }

  public PitBuildAction getPreviousAction() {
    AbstractBuild<?,?> b = owner_;
    while(true) {
      b = b.getPreviousBuild();
      if(b==null)
        return null;
      if(b.getResult() == Result.FAILURE)
        continue;
      PitBuildAction r = b.getAction(PitBuildAction.class);
      if(r != null)
        return r;
    }
  }

  public AbstractBuild<?,?> getOwner() {
    return owner_;
  }

  public ProjectMutations getTarget() {
    return getReport();
  }

  public ProjectMutations getReport() {
    return new ProjectMutations(this);
  }

  public synchronized Map<String, MutationReport> getReports() {
    if (reports_ == null) {
      reports_ = readReports();
    }
    return reports_;
  }

  public String getSourceFileContent(String packageName, String fileName) {
    //can't return inner class content
    if (fileName.contains("$")) {
      return "See main class.";
    }
    try {
      return new TextFile(new File(owner_.getRootDir(), "mutation-report/" + packageName + File.separator + fileName)).read();
    }
    catch (IOException exception) {
      return "Could not read source file: " + owner_.getRootDir().getPath()
              + "/mutation-report/" + packageName + File.separator + fileName + "\n";
    }
  }

  private Map<String, MutationReport> readReports() {
    Map<String, MutationReport> reports = new HashMap<String, MutationReport>();

    try {
      FilePath[] files = new FilePath(owner_.getRootDir()).list("mutation-report*/mutations.xml");

      if (files.length < 1) {
        logger.log(Level.WARNING, "Could not find mutation-report*/mutations.xml in " + owner_.getRootDir());
      }

      for (int i = 0; i < files.length; i++) {
        logger.log(Level.WARNING, "Creating report for file: " + files[i].getRemote());
        reports.put(String.valueOf(i), MutationReport.create(files[i].read()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return reports;
  }

//  public Ratio getKillRatio() {
//    return report_.getKillRatio();
//  }

  public HealthReport getBuildHealth() {
    return new HealthReport((int) getReport().getMutationStats().getKillPercent(),
            Messages._BuildAction_Description(getReport().getMutationStats().getKillPercent()));
  }

  public String getIconFileName() {
    return "/plugin/pitmutation/donatello.png";
  }

  public String getDisplayName() {
    return Messages.BuildAction_DisplayName();
  }

  public String getUrlName() {
    return "pitmutation";
  }

  private static final Logger logger = Logger.getLogger(PitBuildAction.class.getName());

  private AbstractBuild<?, ?> owner_;
  private Map<String, MutationReport> reports_;
}
