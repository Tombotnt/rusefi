package com.rusefi.output;

import com.rusefi.ConfigField;
import com.rusefi.ReaderState;
import com.rusefi.util.LazyFile;

import java.io.IOException;

import static com.rusefi.output.JavaSensorsConsumer.quote;

/**
 * here we tell the firmware what to log on SD card how
 *
 * @see DataLogConsumer
 */
public class SdCardFieldsContent {
    public static final String SD_CARD_OUTPUT_FILE_NAME = "console/binary_log/log_fields_generated.h";
    public static final String BOARD_LOOKUP_H = "#include \"board_lookup.h\"\n";
    private final StringBuilder body = new StringBuilder();

    public String[] expressions = new String[] {"test->reference"}; // technical debt: default value is only used by unit tests
    public String conditional;
    public Boolean isPtr = false;
    public String[] names;
    public int structureStartingTsPosition;

    public static void wrapContent(LazyFile output, String content) {
        output.write("// generated by " + SdCardFieldsContent.class + "\n");
        output.write(BOARD_LOOKUP_H);
        output.write("static const LogField fields[] = {\n" +
                "{packedTime, GAUGE_NAME_TIME, \"sec\", 0},\n");
        output.write(content);
        output.write("};\n");
    }

    public void handleEndStruct(ReaderState state, ConfigStructure structure) throws IOException {
        if (state.isStackEmpty()) {
            for (int i = 0; i < expressions.length; i++) {
                String namePrefix = (names == null || names.length <= 1) ? "" : names[i];
                String expression = expressions[i];
                appendFields(state, structure, namePrefix, expression);
            }
        }
    }

    private void appendFields(ReaderState state, ConfigStructure structure, String namePrefix, String expression) {
        PerFieldWithStructuresIterator.Strategy strategy = new PerFieldWithStructuresIterator.Strategy() {
            @Override
            public String process(ReaderState state, ConfigField configField, String prefix, int currentPosition, PerFieldWithStructuresIterator perFieldWithStructuresIterator) {
                return processOutput(configField, prefix, currentPosition, perFieldWithStructuresIterator, namePrefix, expression, structureStartingTsPosition);
            }

            @Override
            public String getArrayElementName(ConfigField cf) {
                return cf.getOriginalArrayName();
            }
        };
        PerFieldWithStructuresIterator iterator = new PerFieldWithStructuresIterator(state, structure.getTsFields(), "",
                strategy, ".");
        structureStartingTsPosition = iterator.loop(structureStartingTsPosition);
        String content = iterator.getContent();
        body.append(content);
    }

    private String processOutput(ConfigField configField, String prefix, int currentPosition, PerFieldWithStructuresIterator perFieldWithStructuresIterator, String namePrefix, String expression, int structureStartingTsPosition) {
        if (configField.isUnusedField())
            return "";

        String name = configField.getOriginalArrayName();

        return getLine(configField, prefix, namePrefix, prefix + name, expression, isPtr, conditional, currentPosition, perFieldWithStructuresIterator, structureStartingTsPosition);
    }

    private static String getLine(ConfigField configField, String prefix, String namePrefix, String name, String expression, Boolean isPtr, String conditional, int currentPosition, PerFieldWithStructuresIterator perFieldWithStructuresIterator, int structureStartingTsPosition) {
        String humanName = DataLogConsumer.getHumanGaugeName(prefix, configField, namePrefix);
        if (configField.isBit()) {
            // 'structureStartingTsPosition' is about fragment list see fragments.h
            int offsetWithinCurrentStructure = currentPosition - structureStartingTsPosition;
            if (offsetWithinCurrentStructure < 0)
                throw new IllegalStateException(humanName + " seems broken: " + currentPosition + " vs " + structureStartingTsPosition);
            return "// structureStartingTsPosition " + structureStartingTsPosition + " " + expression + "/" + humanName + ", skipping bit " + namePrefix + " at " + currentPosition + " " + offsetWithinCurrentStructure + "@" + perFieldWithStructuresIterator.bitState.get() + "\n";
        }

        String categoryStr = configField.getCategory();

        if (categoryStr == null) {
            categoryStr = "";
        } else {
            categoryStr = ", " + categoryStr;
        }

        boolean isEnum = configField.getTypeName().contains("_e");
        if (isEnum)
            return "";

        String before = conditional == null ? "" : "#if " + conditional + "\n";
        String after = conditional == null ? "" : "#endif\n";

        return before
                + "\t{" +
            expression + (isPtr ? "->" : ".") + name +
                ", "
                + humanName +
                ", " +
                quote(configField.getUnits()) +
                ", " +
                configField.getDigits() +
                categoryStr +
                "},\n" +
                after;
    }

    public String getBody() {
        return body.toString();
    }
}
