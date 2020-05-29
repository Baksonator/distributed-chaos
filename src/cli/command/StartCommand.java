package cli.command;

import app.AppConfig;
import app.Job;
import app.JobCommandHandler;

public class StartCommand implements CLICommand {

    @Override
    public String commandName() {
        return "start";
    }

    @Override
    public void execute(String args) {
        if (args != null) {
            Job job = AppConfig.jobs.stream().filter(job1 -> job1.getName().equals(args)).findFirst().get();
            JobCommandHandler.start(job);
        } else {
            // TODO Ask user for job on CL
        }
    }
}
