package org.tno.networks.args;

import com.lexicalscope.jewel.cli.Option;

public interface AHelp {
	@Option(helpRequest = true, shortName = "h")
	boolean getHelp();
}