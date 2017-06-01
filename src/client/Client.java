package client;

public class Client {

	public static void main(String[] args) {
		if (args.length < 3 || args.length > 4) {
			printUsage();
			return;
		}

		String server_address = args[0];
		String server_port = args[1];
		String operation = args[2].toUpperCase();
		String operative = null;
		if (args.length == 4) {
			operative = args[3];
		}
		

		switch (operation) {
		case "UPLOAD":
		case "DOWNLOAD":
		case "DELETE":
			if (args.length < 4) {
				printUsage();
				return;
			}
			break;
		case "LIST":
			if (args.length > 3) {
				printUsage();
				return;
			}
			break;
		default:
			System.out.println("Operations supported are: upload, download, list and delete.");
			return;
		}

		Action action = new Action(server_address, server_port, operation, operative);
		action.run();
	}

	private static void printUsage() {
		System.out.println("Usage: java Client <srv_addr> <srv_port> <oper> <opnd>*");
		System.out.println("Where:");
		System.out.println("	<srv_addr> Server address you are accessing");
		System.out.println("	<srv_port> The port the server is using");
		System.out.println("	<oper> The service you wish.");
		System.out.println("		Upload, Download, List or Delete");
		System.out.println("	<opnd> * is the list of operands of the specified operation:");
		System.out.println("		<file_path> for Upload");
		System.out.println("		<file_ID> Download and Delete");
		System.out.println("		no other operand needed for List.");
	}
}
