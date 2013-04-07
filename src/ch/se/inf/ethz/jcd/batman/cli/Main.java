package ch.se.inf.ethz.jcd.batman.cli;

import java.io.IOException;

import ch.se.inf.ethz.jcd.batman.cli.command.ChangeDirectoryCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.CommandNotFound;
import ch.se.inf.ethz.jcd.batman.cli.command.CreateCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.CreateDirectoryCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.DeleteCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.DestroyCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.ExportCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.ImportCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.ListMembersCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.LoadCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.MoveCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.QueryCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.SizeCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.StopCommand;
import ch.se.inf.ethz.jcd.batman.cli.command.UnloadCommand;

/**
 * Starts the CLI interface.
 *
 */
public class Main {

	public static void main(String[] args) throws IOException {
		CommandLineInterface cli = new CommandLineInterface();
		
		// register commands
		cli.addObserver(new StopCommand());
		cli.addObserver(new CommandNotFound());
		cli.addObserver(new CreateCommand());
		cli.addObserver(new LoadCommand());
		cli.addObserver(new UnloadCommand());
		cli.addObserver(new ListMembersCommand());
		cli.addObserver(new ChangeDirectoryCommand());
		cli.addObserver(new CreateDirectoryCommand());
		cli.addObserver(new MoveCommand());
		cli.addObserver(new SizeCommand());
		cli.addObserver(new ImportCommand());
		cli.addObserver(new ExportCommand());
		cli.addObserver(new DeleteCommand());
		cli.addObserver(new QueryCommand());
		cli.addObserver(new DestroyCommand());
		
		// start command line interface
		cli.start();
	}

}
