package minerful;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import minerful.concept.TaskChar;
import minerful.concept.TaskCharArchive;
import minerful.concept.constraint.TaskCharRelatedConstraintsBag;
import minerful.logparser.LogEventClassifier.ClassificationType;
import minerful.logparser.LogParser;
import minerful.logparser.StringLogParser;
import minerful.logparser.XesLogParser;
import minerful.miner.call.MinerFulKBCore;
import minerful.miner.call.MinerFulQueryingCore;
import minerful.miner.params.MinerFulCmdParameters;
import minerful.miner.stats.GlobalStatsTable;
import minerful.params.InputCmdParameters;
import minerful.params.SystemCmdParameters;
import minerful.params.ViewCmdParameters;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class MinerFulMinerStarter extends AbstractMinerFulStarter {

	public static final String MINERFUL_XMLNS = "https://github.com/cdc08x/MINERful/";

	@Override
	public Options setupOptions() {
		Options cmdLineOptions = new Options();

		Options minerfulOptions = MinerFulCmdParameters.parseableOptions(), inputOptions = InputCmdParameters
				.parseableOptions(), systemOptions = SystemCmdParameters
				.parseableOptions(), viewOptions = ViewCmdParameters
				.parseableOptions();

		for (Object opt : minerfulOptions.getOptions()) {
			cmdLineOptions.addOption((Option) opt);
		}
		for (Object opt : inputOptions.getOptions()) {
			cmdLineOptions.addOption((Option) opt);
		}
		for (Object opt : viewOptions.getOptions()) {
			cmdLineOptions.addOption((Option) opt);
		}
		for (Object opt : systemOptions.getOptions()) {
			cmdLineOptions.addOption((Option) opt);
		}

		return cmdLineOptions;
	}

	/**
	 * @param args
	 *            the command line arguments: [regular expression] [number of
	 *            strings] [minimum number of characters per string] [maximum
	 *            number of characters per string] [alphabet]...
	 */
	public static void main(String[] args) {
		MinerFulMinerStarter minerMinaStarter = new MinerFulMinerStarter();
		Options cmdLineOptions = minerMinaStarter.setupOptions();

		InputCmdParameters inputParams = new InputCmdParameters(cmdLineOptions,
				args);
		MinerFulCmdParameters minerFulParams = new MinerFulCmdParameters(
				cmdLineOptions, args);
		ViewCmdParameters viewParams = new ViewCmdParameters(cmdLineOptions,
				args);
		SystemCmdParameters systemParams = new SystemCmdParameters(
				cmdLineOptions, args);

		if (systemParams.help) {
			systemParams.printHelp(cmdLineOptions);
			System.exit(0);
		}
		if (inputParams.inputFile == null) {
			systemParams.printHelpForWrongUsage("Input file missing!",
					cmdLineOptions);
			System.exit(1);
		}

		configureLogging(systemParams.debugLevel);

		logger.info("Loading log...");

		LogParser logParser = deriveLogParserFromLogFile(inputParams,
				minerFulParams);

		TaskCharArchive taskCharArchive = logParser.getTaskCharArchive();

		TaskCharRelatedConstraintsBag bag = minerMinaStarter.mine(logParser,
				minerFulParams, viewParams, systemParams, taskCharArchive);

		new MinerFulProcessViewerStarter().print(bag, viewParams, systemParams,
				logParser);

		if (minerFulParams.processSchemeOutputFile != null) {
			File procSchmOutFile = minerFulParams.processSchemeOutputFile;
			try {
				marshalMinedProcessScheme(bag, procSchmOutFile);
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

	private static LogParser deriveLogParserFromLogFile(
			InputCmdParameters inputParams, MinerFulCmdParameters minerFulParams) {
		LogParser logParser = null;
		switch (inputParams.inputLanguage) {
		case xes:
			ClassificationType evtClassi = MinerFulLauncher
					.fromInputParamToXesLogClassificationType(inputParams.eventClassification);
			try {
				logParser = new XesLogParser(inputParams.inputFile, evtClassi);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// Remove from the analysed alphabet those activities that are
			// specified in a user-defined list
			logParser.getEventEncoderDecoder().excludeThese(
					minerFulParams.activitiesToExcludeFromResult);

			// Let us try to free memory from the unused XesDecoder!
			System.gc();
			break;
		case strings:
			try {
				logParser = new StringLogParser(inputParams.inputFile,
						ClassificationType.NAME);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			break;
		default:
			throw new UnsupportedOperationException("This encoding ("
					+ inputParams.inputLanguage + ") is not supported yet");
		}

		return logParser;
	}

	public static void marshalMinedProcessScheme(
			TaskCharRelatedConstraintsBag bag, File procSchmOutFile)
			throws JAXBException, PropertyException, FileNotFoundException,
			IOException {
		String pkgName = bag.getClass().getCanonicalName().toString();
		pkgName = pkgName.substring(0, pkgName.lastIndexOf('.'));
		JAXBContext jaxbCtx = JAXBContext.newInstance(pkgName);
		Marshaller marsh = jaxbCtx.createMarshaller();
		marsh.setProperty("jaxb.formatted.output", true);
		StringWriter strixWriter = new StringWriter();
		marsh.marshal(bag, strixWriter);
		strixWriter.flush();
		StringBuffer strixBuffer = strixWriter.getBuffer();

		// OINK
		strixBuffer.replace(
				strixBuffer.indexOf(">", strixBuffer.indexOf("?>") + 3),
				strixBuffer.indexOf(">", strixBuffer.indexOf("?>") + 3),
				" xmlns=\"" + MinerFulMinerStarter.MINERFUL_XMLNS + "\"");
		FileWriter strixFileWriter = new FileWriter(procSchmOutFile);
		strixFileWriter.write(strixBuffer.toString());
		strixFileWriter.flush();
		strixFileWriter.close();
	}

	public TaskCharRelatedConstraintsBag mine(LogParser logParser,
			MinerFulCmdParameters minerFulParams, ViewCmdParameters viewParams,
			SystemCmdParameters systemParams, Character[] alphabet) {
		TaskCharArchive taskCharArchive = new TaskCharArchive(alphabet);
		return this.mine(logParser, minerFulParams, viewParams, systemParams,
				taskCharArchive);
	}

	public TaskCharRelatedConstraintsBag mine(LogParser logParser,
			MinerFulCmdParameters minerFulParams, ViewCmdParameters viewParams,
			SystemCmdParameters systemParams, TaskCharArchive taskCharArchive) {
		GlobalStatsTable globalStatsTable = new GlobalStatsTable(taskCharArchive,minerFulParams.branchingLimit);

		globalStatsTable = computeKB(logParser, minerFulParams,
				taskCharArchive, globalStatsTable);
		System.gc();
// System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + globalStatsTable);
		// TODO Probably, parameters should be differentiated. It looks weird
		// that the same number of parallel threads suits both the computation
		// of the KB and the querying.
		return queryForConstraints(logParser, minerFulParams, viewParams,
				taskCharArchive, globalStatsTable);
	}

	private GlobalStatsTable computeKB(LogParser logParser,
			MinerFulCmdParameters minerFulParams,
			TaskCharArchive taskCharArchive, GlobalStatsTable globalStatsTable) {
		int coreNum = 0;
		long before = 0, after = 0;
		if (minerFulParams.kbParallelProcessingThreads > MinerFulCmdParameters.MINIMUM_PARALLEL_EXECUTION_THREADS) {
			// Slice the log
			List<LogParser> listOfLogParsers = logParser
					.split(minerFulParams.kbParallelProcessingThreads);
			List<MinerFulKBCore> listOfMinerFulCores = new ArrayList<MinerFulKBCore>(
					minerFulParams.kbParallelProcessingThreads);

			// Associate a dedicated KB-computing core to each log slice
			for (LogParser slicedLogParser : listOfLogParsers) {
				listOfMinerFulCores.add(new MinerFulKBCore(
						coreNum++,
						slicedLogParser,
						minerFulParams, taskCharArchive));
			}

//			ExecutorService executor = Executors
//					.newFixedThreadPool(minerFulParams.parallelProcessingThreads);
			
			ForkJoinPool executor = new ForkJoinPool(minerFulParams.kbParallelProcessingThreads);
			
			try {
				before = System.currentTimeMillis();
				for (Future<GlobalStatsTable> statsTab : executor
						.invokeAll(listOfMinerFulCores)) {
					globalStatsTable.merge(statsTab.get());
				}
				after = System.currentTimeMillis();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				System.exit(1);
			}
			executor.shutdown();
		} else {
			MinerFulKBCore minerFulKbCore = new MinerFulKBCore(
					coreNum++,
					logParser,
					minerFulParams, taskCharArchive);
			before = System.currentTimeMillis();
			globalStatsTable = minerFulKbCore.discover();
			after = System.currentTimeMillis();
		}
		logger.info("Total KB construction time: " + (after - before));
		return globalStatsTable;
	}

	private TaskCharRelatedConstraintsBag queryForConstraints(
			LogParser logParser, MinerFulCmdParameters minerFulParams,
			ViewCmdParameters viewParams, TaskCharArchive taskCharArchive,
			GlobalStatsTable globalStatsTable) {
		int coreNum = 0;
		TaskCharRelatedConstraintsBag bag = new TaskCharRelatedConstraintsBag();
		long before = 0, after = 0;
		if (minerFulParams.queryParallelProcessingThreads > MinerFulCmdParameters.MINIMUM_PARALLEL_EXECUTION_THREADS) {
			Collection<Set<TaskChar>> taskCharSubSets =
					taskCharArchive.splitTaskCharsIntoSubsets(
							minerFulParams.queryParallelProcessingThreads);
			List<MinerFulQueryingCore> listOfMinerFulCores =
					new ArrayList<MinerFulQueryingCore>(
							minerFulParams.queryParallelProcessingThreads);
			
			// Associate a dedicated query-computing core to each taskChar-subset
			for (Set<TaskChar> taskCharSubset : taskCharSubSets) {
				listOfMinerFulCores.add(
						new MinerFulQueryingCore(coreNum++,
								logParser, minerFulParams, viewParams,
								taskCharArchive, globalStatsTable, taskCharSubset));
			}
			
//			ExecutorService executor = Executors
//					.newFixedThreadPool(minerFulParams.parallelProcessingThreads);
			ForkJoinPool executor = new ForkJoinPool(minerFulParams.queryParallelProcessingThreads);
			
			try {
				before = System.currentTimeMillis();
				for (Future<TaskCharRelatedConstraintsBag> subBag : executor
						.invokeAll(listOfMinerFulCores)) {
					bag.merge(subBag.get());
				}
				after = System.currentTimeMillis();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				System.exit(1);
			}
			executor.shutdown();
		} else {
			MinerFulQueryingCore minerFulQueryingCore = new MinerFulQueryingCore(coreNum++,
					logParser, minerFulParams, viewParams, taskCharArchive,
					globalStatsTable);
			before = System.currentTimeMillis();
			bag = minerFulQueryingCore.discover();
			after = System.currentTimeMillis();
		}
		logger.info("Total querying time: " + (after - before));
		return bag;
	}
}