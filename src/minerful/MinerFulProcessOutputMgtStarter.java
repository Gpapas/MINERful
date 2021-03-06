package minerful;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.NavigableMap;

import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;

import minerful.concept.ProcessModel;
import minerful.concept.constraint.Constraint;
import minerful.concept.constraint.ConstraintsBag;
import minerful.index.LinearConstraintsIndexFactory;
import minerful.io.ConstraintsPrinter;
import minerful.io.encdec.ProcessModelEncoderDecoder;
import minerful.io.params.OutputModelParameters;
import minerful.logparser.LogParser;
import minerful.params.SystemCmdParameters;
import minerful.params.ViewCmdParameters;
import minerful.utils.MessagePrinter;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class MinerFulProcessOutputMgtStarter extends AbstractMinerFulStarter {

	@Override
	public Options setupOptions() {
		Options cmdLineOptions = new Options();
		
		Options systemOptions = SystemCmdParameters.parseableOptions(),
				outputOptions = OutputModelParameters.parseableOptions(),
				viewOptions = ViewCmdParameters.parseableOptions();
    	for (Object opt: viewOptions.getOptions()) {
    		cmdLineOptions.addOption((Option)opt);
    	}
    	for (Object opt: outputOptions.getOptions()) {
    		cmdLineOptions.addOption((Option)opt);
    	}
    	for (Object opt: systemOptions.getOptions()) {
    		cmdLineOptions.addOption((Option)opt);
    	}
		
		return cmdLineOptions;
	}

	// FIXME	To be developed
	public static void main(String[] args) {
		MinerFulProcessOutputMgtStarter procViewStarter = new MinerFulProcessOutputMgtStarter();
    	Options cmdLineOptions = procViewStarter.setupOptions();

        ViewCmdParameters viewParams =
        		new ViewCmdParameters(
        				cmdLineOptions,
        				args);
		OutputModelParameters outParams =
				new OutputModelParameters(
						cmdLineOptions,
						args);
        SystemCmdParameters systemParams =
        		new SystemCmdParameters(
        				cmdLineOptions,
    					args);

        if (systemParams.help) {
        	systemParams.printHelp(cmdLineOptions);
        	System.exit(0);
        }

	}

	public void manageOutput(ProcessModel processModel, NavigableMap<Constraint, String> additionalCnsIndexedInfo, OutputModelParameters outParams, ViewCmdParameters viewParams, SystemCmdParameters systemParams, LogParser logParser) {
		ConstraintsPrinter printer = new ConstraintsPrinter(processModel, additionalCnsIndexedInfo);
		PrintWriter outWriter = null;

		if (viewParams.machineReadableResults) {
        	logger.info(printer.printBagAsMachineReadable(
        			(systemParams.debugLevel.compareTo(
        					SystemCmdParameters.DebugLevel.debug) >= 0)
					)
			);
        }
        if (outParams.fileToSaveConstraintsCsv != null) {
			logger.info("Saving discovered constraints in CSV format as " + outParams.fileToSaveConstraintsCsv + "...");

        	try {
    				outWriter = new PrintWriter(outParams.fileToSaveConstraintsCsv);
    	        	outWriter.print(printer.printBagCsv());
    	        	outWriter.flush();
    	        	outWriter.close();
    			} catch (FileNotFoundException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        }
        if (outParams.fileToSaveConDecDefinition != null) {
        	logger.info("Saving discovered process model in ConDec/Declare-map XML format as " + outParams.fileToSaveConDecDefinition + "...");
        	try {
				printer.printConDecModel(outParams.fileToSaveConDecDefinition);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        ConstraintsBag bagClone = null;
        switch (viewParams.constraintsSorting) {
		case support:
			bagClone = LinearConstraintsIndexFactory.createConstraintsBagCloneIndexedByTaskCharAndSupport(processModel.bag);
			break;
		case interest:
			bagClone = LinearConstraintsIndexFactory.createConstraintsBagCloneIndexedByTaskCharAndInterest(processModel.bag);
		case type:
		default:
			break;
		}

        if (viewParams.noFoldingRequired) {
        	switch (viewParams.constraintsSorting) {
        	case interest:
        		MessagePrinter.printlnOut(printer.printUnfoldedBagOrderedByInterest());
        	case support:
        		MessagePrinter.printlnOut(printer.printUnfoldedBagOrderedBySupport());
        		break;
        	case type:
    		default:
    			MessagePrinter.printlnOut(printer.printUnfoldedBag());
    			break;
        	}
        } else {
        	MessagePrinter.printlnOut(printer.printBag());
        }

		if (outParams.fileToSaveDotFileForAutomaton != null) {
        	try {
				outWriter = new PrintWriter(outParams.fileToSaveDotFileForAutomaton);
	        	outWriter.print(printer.printDotAutomaton());
	        	outWriter.flush();
	        	outWriter.close();
	        	MessagePrinter.printlnOut("Discovered process automaton written in DOT format on " + outParams.fileToSaveDotFileForAutomaton);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

		if (outParams.fileToSaveTsmlFileForAutomaton != null) {
        	try {
        		outWriter = new PrintWriter(new File(outParams.fileToSaveTsmlFileForAutomaton.getAbsolutePath()));
	        	outWriter.print(printer.printTSMLAutomaton());
	        	outWriter.flush();
	        	outWriter.close();
	        	MessagePrinter.printlnOut("Discovered process automaton written in TSML format on " + outParams.fileToSaveTsmlFileForAutomaton);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (logParser != null) {
			if (outParams.fileToSaveXmlFileForAutomaton != null) {
	        	try {
	        		outWriter = new PrintWriter(new File(outParams.fileToSaveXmlFileForAutomaton.getAbsolutePath()));
		        	outWriter.print(printer.printWeightedXmlAutomaton(logParser));
		        	outWriter.flush();
		        	outWriter.close();
		        	MessagePrinter.printlnOut("Discovered weighted process automaton written in XML format on " + outParams.fileToSaveXmlFileForAutomaton);
				} catch (FileNotFoundException | JAXBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	
			if (outParams.folderToSaveXmlFilesForPartialAutomata != null) {
	        	try {
	        		NavigableMap<String, String> partialAutoMap = printer.printWeightedXmlSubAutomata(logParser);
					StringBuilder subAutomataPathsBuilder = new StringBuilder();
					String subAutomatonPath = null;
					
					for (Map.Entry<String, String> partialAutomaton : partialAutoMap.entrySet()) {
						try {
							subAutomatonPath = outParams.folderToSaveXmlFilesForPartialAutomata
									+ "/"
									+ partialAutomaton.getKey().replaceAll("\\W", "_")
									+ ".automaton.xml";
							outWriter = new PrintWriter(
									subAutomatonPath
									);
							outWriter.print(partialAutomaton.getValue());
							outWriter.flush();
				        	outWriter.close();
				        	
				        	subAutomataPathsBuilder.append("Sub-automaton for activity \"");
				        	subAutomataPathsBuilder.append(partialAutomaton.getKey());
				        	subAutomataPathsBuilder.append("\" written in XML format on ");
				        	subAutomataPathsBuilder.append(subAutomatonPath);
				        	subAutomataPathsBuilder.append('\n');
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					MessagePrinter.printlnOut(subAutomataPathsBuilder.toString());
				} catch (JAXBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (outParams.folderToSaveDotFilesForPartialAutomata != null) {
			NavigableMap<String, String> partialAutoMap = printer.printDotPartialAutomata();
			StringBuilder subAutomataPathsBuilder = new StringBuilder();
			String subAutomatonPath = null;
			
			logger.info("Saving activation-related automata DOT files in folder " + outParams.folderToSaveDotFilesForPartialAutomata + "...");

			for (Map.Entry<String, String> partialAutomaton : partialAutoMap.entrySet()) {
				try {
					subAutomatonPath = outParams.folderToSaveDotFilesForPartialAutomata
							+ "/"
							+ partialAutomaton.getKey().replaceAll("\\W", "_")
							+ ".automaton.dot";
					outWriter = new PrintWriter(
							subAutomatonPath
							);
					outWriter.print(partialAutomaton.getValue());
					outWriter.flush();
		        	outWriter.close();
		        	
		        	subAutomataPathsBuilder.append("Sub-automaton for activity \"");
		        	subAutomataPathsBuilder.append(partialAutomaton.getKey());
		        	subAutomataPathsBuilder.append("\" written in DOT format on ");
		        	subAutomataPathsBuilder.append(subAutomatonPath);
		        	subAutomataPathsBuilder.append('\n');
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			MessagePrinter.printlnOut(subAutomataPathsBuilder.toString());
		}

		if (outParams.processModelOutputFile != null) {
			File processModelOutFile = outParams.processModelOutputFile;
			logger.info("Saving the discovered process as " + processModelOutFile + "...");

			try {
				new ProcessModelEncoderDecoder().marshalProcessModel(processModel, processModelOutFile);
			} catch (PropertyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void manageOutput(ProcessModel processModel,
			ViewCmdParameters viewParams, OutputModelParameters outParams, SystemCmdParameters systemParams,
			LogParser logParser) {
		this.manageOutput(processModel, null, outParams, viewParams, systemParams, logParser);
	}

	public void manageOutput(ProcessModel processModel,
			ViewCmdParameters viewParams, OutputModelParameters outParams, SystemCmdParameters systemParams) {
		this.manageOutput(processModel, null, outParams, viewParams, systemParams, null);
	}
}