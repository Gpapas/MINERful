/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minerful.stringsmaker.params;

import java.io.File;

import minerful.io.encdec.TaskCharEncoderDecoder;
import minerful.logmaker.params.LogMakerCmdParameters;
import minerful.params.ParamsManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class StringTracesMakerCmdParameters extends ParamsManager {
	public static final String OUTPUT_FILE_PARAM_NAME = "oLF";
    public static final String OUT_ENC_PARAM_NAME = "oE";
	public static final char SIZE_PARAM_NAME = 'L';
	public static final char MAX_LEN_PARAM_NAME = 'M';
	public static final char MIN_LEN_PARAM_NAME = 'm';
	public static final char ALPHABET_PARAM_NAME = 'a';
	public static final char REG_EXPS_PARAM_NAME = 'r';

	public static final String ALPHABET_CHARACTERS_SEPARATOR = ":";
    public static final Long SIZE = 100L;
    public static final Character[] TEST_ALPHABET = 
            {'n', 'p', 'r', 'c'};
    public static final String TEST_REGEXP =
            //      "[bcdef]*((a[acdef]*b)|(b[bcdef]*a))+[bcdef]*";
            //      "[bc]*((a[ac]*b)|(b[bc]*a))+[b]*";

            "("
            +
            // 		"[prc]*(n[prc]*)+[prc]*" + // Participation(n)
            //    	")&(" +
            //		"[prc]*(n)?[prc]*" + // Once(n)
            //		")&(" +
            //		"[nprc]*n" + // End(n)
            //   	")&(" +
            "[rc]*(p[nprc]*n)*[rc]*" + // Succession(p, n)
            //   	")&(" +
            //		"[npc]*(r[nprc]p)*[npc]*" + // Response(r, p)
            //    	")&(" +
            //    	"[npr]*((c[nprc]*p)|(p[nprc]*c))*[npr]*" + // RespondedExistence(c, p)
            //     	"[npr]*(r[npr]*c)*[npr]*" + // AlternatePrecedence(r, c); ^[^s]*(r[^s]*s)*[^s]*$
            // 		"n[nrc]+[nprc]+c" +
            ")";
    public static final Integer MIN_OCCURRENCES = 0;
    public static final Integer MAX_OCCURRENCES = Integer.MAX_VALUE;
            
    public String[] regexps;
    public Character[] alphabet;
    public Integer minChrsPerString;
    public Integer maxChrsPerString;
    public Long size;
    public File logFile;
    public LogMakerCmdParameters.Encoding outputEncoding;
    
    public StringTracesMakerCmdParameters() {
    	super();
    	regexps = new String[]{TEST_REGEXP};
        alphabet = TEST_ALPHABET;
        minChrsPerString = MIN_OCCURRENCES;
        maxChrsPerString = MAX_OCCURRENCES;
        size = SIZE;
        logFile = null;
        outputEncoding = LogMakerCmdParameters.Encoding.string;
    }
    
    public StringTracesMakerCmdParameters(Options options, String[] args) {
    	this();
        // parse the command line arguments
    	this.parseAndSetup(options, args);
	}

	public StringTracesMakerCmdParameters(String[] args) {
    	this();
        // parse the command line arguments
    	this.parseAndSetup(new Options(), args);
	}

	@Override
	protected void setup(CommandLine line) {
        // validate that block-size has been set
        this.regexps = line.getOptionValues(StringTracesMakerCmdParameters.REG_EXPS_PARAM_NAME);
        if (this.regexps == null)
            this.regexps = new String[]{TEST_REGEXP};
        if (line.getOptionValues(StringTracesMakerCmdParameters.ALPHABET_PARAM_NAME) != null) {
            this.alphabet = TaskCharEncoderDecoder.faultyEncode(line.getOptionValue(StringTracesMakerCmdParameters.ALPHABET_PARAM_NAME).toString().split(ALPHABET_CHARACTERS_SEPARATOR));
        }
        this.minChrsPerString =
        		Integer.valueOf(
        				line.getOptionValue(StringTracesMakerCmdParameters.MIN_LEN_PARAM_NAME,this.minChrsPerString.toString()));
        this.maxChrsPerString =
        		Integer.valueOf(
        				line.getOptionValue(StringTracesMakerCmdParameters.MAX_LEN_PARAM_NAME, this.maxChrsPerString.toString()));
        this.size =
        		Long.valueOf(line.getOptionValue(StringTracesMakerCmdParameters.SIZE_PARAM_NAME, this.size.toString()));
        this.outputEncoding = Enum.valueOf(
        		LogMakerCmdParameters.Encoding.class,
        		line.getOptionValue(OUT_ENC_PARAM_NAME, this.outputEncoding.toString())
		);
        if (line.hasOption(StringTracesMakerCmdParameters.OUTPUT_FILE_PARAM_NAME)) {
        	this.logFile = new File(line.getOptionValue(StringTracesMakerCmdParameters.OUTPUT_FILE_PARAM_NAME));
        }
	}
    
	@Override
    public Options addParseableOptions(Options options) {
		Options myOptions = listParseableOptions();
		for (Object myOpt: myOptions.getOptions())
			options.addOption((Option)myOpt);
        return options;
	}
	
	@Override
    public Options listParseableOptions() {
    	return parseableOptions();
    }
	@SuppressWarnings("static-access")
	public static Options parseableOptions() {
		Options options = new Options();
        options.addOption(
                OptionBuilder
                .hasArgs().withArgName("reg exp list")
                .withLongOpt("regexp")
                .withDescription("unbound regular expressions list generating the strings (in conjunction)")
                .withType(new String[0])
                .create(StringTracesMakerCmdParameters.REG_EXPS_PARAM_NAME)
        );
        options.addOption(
                OptionBuilder
                .hasArg().withArgName("alphabet")
                .withLongOpt("alphabet")
                .withDescription("\"" + ALPHABET_CHARACTERS_SEPARATOR + "\"-separated list of characters in the alphabet (e.g., a:b:c)")
                .create(StringTracesMakerCmdParameters.ALPHABET_PARAM_NAME)
        );
        options.addOption(
                OptionBuilder
                .hasArg().withArgName("min_length")
                .withLongOpt("minlen")
                .withDescription("minimum length of the generated strings")
                .withType(new Integer(0))
                .create(StringTracesMakerCmdParameters.MIN_LEN_PARAM_NAME)
        );
        options.addOption(
                OptionBuilder
                .hasArg().withArgName("max_length")
                .withLongOpt("maxlen")
                .withDescription("maximum length of the generated strings")
                .withType(new Integer(0))
                .create(StringTracesMakerCmdParameters.MAX_LEN_PARAM_NAME)
        );
        options.addOption(
                OptionBuilder
                .hasArg().withArgName("number of strings")
                .withLongOpt("size")
                .withDescription("number of strings to run on")
                .withType(new Double(0))
                .create(StringTracesMakerCmdParameters.SIZE_PARAM_NAME)
        );
        options.addOption(
                OptionBuilder
                .hasArg().withArgName("file path")
                .withLongOpt("out-log")
                .withDescription("path to the file to write the log in")
                .withType(new String())
                .create(StringTracesMakerCmdParameters.OUTPUT_FILE_PARAM_NAME)
    	);
        options.addOption(
                OptionBuilder
                .hasArg().withArgName("encoding")
                .withLongOpt("out-enc")
                .withDescription("encoding language for output log " + printValues(LogMakerCmdParameters.Encoding.values()))
                .withType(new String())
                .create(StringTracesMakerCmdParameters.OUT_ENC_PARAM_NAME)
    	);
        
        return options;
    }

    public int getNumberOfConstraints() {
        return this.regexps.length;
    }
    
    public String printAlphabet() {
        StringBuilder alphabetStringBuffer = new StringBuilder();
        alphabetStringBuffer.append("{");
        for (Character chr: this.alphabet) {
            alphabetStringBuffer.append(chr);
            alphabetStringBuffer.append(", ");
        }
        return alphabetStringBuffer.substring(0, alphabetStringBuffer.length() -2) + "}";
    }
    
    public String printMaxChrsPerString() {
        return (this.isMaxChrsPerStringGiven() ? String.valueOf(this.maxChrsPerString) : "*");
    }
    public String printMinChrsPerString() {
        return (this.isMinChrsPerStringGiven() ? String.valueOf(this.minChrsPerString) : "0");
    }
    public String printRegExps() {
        StringBuffer regExpsStringBuffer = new StringBuffer();
        regExpsStringBuffer.append("\n{");
        for (String re: this.regexps) {
            regExpsStringBuffer.append("\n\t");
            regExpsStringBuffer.append(re);
            regExpsStringBuffer.append(",");
        }
        return regExpsStringBuffer.substring(0, regExpsStringBuffer.length() -1) + "\n}";
    }
    
    public boolean isMinChrsPerStringGiven() {
        return (this.minChrsPerString > 0);
    }

    public boolean isMaxChrsPerStringGiven() {
        return (this.maxChrsPerString < Integer.MAX_VALUE);
    }
}