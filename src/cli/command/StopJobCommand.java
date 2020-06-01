package cli.command;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;

public class StopJobCommand implements CLICommand {

    @Override
    public String commandName() {
        return "stopJob";
    }

    @Override
    public void execute(String args) {
        Job job = AppConfig.jobs.stream().filter(job1 -> job1.getName().equals(args)).findFirst().get();
        JobCommandHandler.stop(job);
    }
}
