package cli.command;

import app.AppConfig;
import app.ServentInfo;

import java.util.List;

public class SuccessorInfo implements CLICommand {

	@Override
	public String commandName() {
		return "successor_info";
	}

	@Override
	public void execute(String args) {
		List<ServentInfo> successorTable = AppConfig.chordState.getSuccessorTableAlt();
//		ServentInfo[] successorTable = AppConfig.chordState.getSuccessorTable();

		System.out.println(AppConfig.myServentInfo.getUuid());
		int num = 0;
		for (ServentInfo serventInfo : successorTable) {
			System.out.println(num + ": " + serventInfo);
			num++;
		}

	}

}
