package cli.command;

public class StatusCommand implements CLICommand {

    @Override
    public String commandName() {
        return "status";
    }

    @Override
    public void execute(String args) {
        if (args == null) {

        } else if (args.split(" ").length == 1) {

        } else {

        }
    }
}
