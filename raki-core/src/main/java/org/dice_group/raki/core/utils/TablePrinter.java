package org.dice_group.raki.core.utils;

import java.util.List;

/**
 * Helper class to print Information as a nice table.
 */
public class TablePrinter {

    /**
     * Prints the provided Table and the header using a column width of 15 and represented as string.
     *
     * @param table The table to print
     * @param header The corresponding header to print
     */
    public static void print(List<List<Object>> table, List<String> header) {
        StringBuilder formatString = new StringBuilder();
        for (String ignored : header) {
            formatString.append("%15s ");
        }
        print(table, header, formatString.toString());
    }

    /**
     * Prints the table and header using the formatString.
     *
     * The formatString needs exactly as many elements as the header and table row elements have.
     *
     * Example: "%10s %20s %d" for the table content "Name" "Description" "Some Integer value"
     *
     * Be aware that this automatically replaces non string formats to string for the header.
     *
     * <h3>Example:</h3>
     * <pre>
     *     {@code
     *         List<String> header = new ArrayList<>();
     *         header.add("Name");
     *         header.add("Description");
     *         header.add("Salary");
     *
     *         List<List<Object>> table = new ArrayList<>();
     *         table.add(Lists.newArrayList("Joe", "Consultant in IT", 5000));
     *         table.add(Lists.newArrayList("Mary", "Engineer in IT", 8000));
     *         table.add(Lists.newArrayList("Lina", "Security Expert in IT", 9500));
     *         TablePrinter.print(table, header, "%10s %20s %5d");
     *     }
     *
     *     Output:
     * ---------------------------------------------------------------------------
     *       Name          Description Salary
     * ---------------------------------------------------------------------------
     *        Joe     Consultant in IT  5000
     *       Mary       Engineer in IT  8000
     *       Lina Security Expert in IT  9500
     * ---------------------------------------------------------------------------
     *
     * </pre>
     *
     * @param table the table to print
     * @param header the header to print
     * @param formatString the format string to use for each table row.
     */
    public static void print(List<List<Object>> table, List<String> header, String formatString){
        String headerFormatString = formatString.replaceAll("(%[0-9]*).", "$1s");
        System.out.println("---------------------------------------------------------------------------");
        System.out.format(headerFormatString, header.toArray());
        System.out.println();
        System.out.println("---------------------------------------------------------------------------");
        for(List<Object> row : table){
            System.out.format(formatString, row.toArray());
            System.out.println();
        }
        System.out.println("---------------------------------------------------------------------------");
    }
}
