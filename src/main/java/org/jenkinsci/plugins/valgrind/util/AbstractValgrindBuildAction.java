package org.jenkinsci.plugins.valgrind.util;

import java.io.IOException;

import jenkins.model.RunAction2;
import hudson.model.Actionable;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.model.Run;

import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public abstract class AbstractValgrindBuildAction extends Actionable implements RunAction2, HealthReportingAction, StaplerProxy
{
    protected transient Run<?, ?> owner;

    protected AbstractValgrindBuildAction(Run<?, ?> owner) {
        this.owner = owner;
    }

    @SuppressWarnings("unchecked")
	public <T extends AbstractValgrindBuildAction> T getPreviousResult() {
        Run<?, ?> b = owner;
        while (true) {
            b = b.getPreviousBuild();
            if (b == null)
                return null;
            if (b.getResult() == Result.FAILURE)
                continue;
            AbstractValgrindBuildAction r = b.getAction(this.getClass());
            if (r != null)
                return (T) r;
        }
    }

    public Run<?, ?> getOwner() {
        return owner;
    }
    
    public void setOwner(Run<?, ?> owner) {
        this.owner = owner;
    }

    public void onAttached(Run<?,?> r) {
        setOwner(r);
    }

    public void onLoad(Run<?,?> r) {
        setOwner(r);
    }

    public abstract void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException;
}
