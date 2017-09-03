package xlreader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * This class represents a set of excel documents.
 *
 * So, basically it's a bunch of sheets. Each sheet
 * has some rows and columns. A row and a column
 * specify a cell. Cells contain different types
 * of data: numbers, formula, strings, etc...
 *
 * An XlReader is constructed with a filename.
 */
public class XlReader {
    private final String filename;
    private final Workbook workbook;
    private final HashMap<Integer, Sheet> sheets;
    private final int nsheets;

    public XlReader(final String filename) throws IOException {
        this.filename = filename;
        this.workbook = makeWorkBooks();
        this.sheets = getSheets();
        this.nsheets = this.workbook.getNumberOfSheets();
    }

    /**
     * Return a HashMap of sheets.
     *
     * @return  HashMap<Integer, Sheet>
     */
    private HashMap<Integer, Sheet> getSheets() {
        HashMap<Integer, Sheet> hashmap = new HashMap<>();
        for (int idx = 0; idx < nsheets; idx++) {
            hashmap.put(idx, this.workbook.getSheetAt(idx));
        }

        return hashmap;
    }

    /**
     * Open an excel file and return a Workbook.
     *
     * @return                  an apache.poi.ss.Workbook
     * @throws  IOException     if the file doesn't exist or something like that
     */
    private Workbook makeWorkBooks() throws IOException {
        FileInputStream fin = null;

        /* Try to open the file. If this fails exit with an error code of 1.*/
        try {
            fin = new FileInputStream(this.filename);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        Workbook wb = null;

        try {
            String ext = filename.substring(filename.indexOf("."));

            if (ext.equals(".xls")) {
                wb = new HSSFWorkbook(fin);
            }
            else if (ext.equals(".xlsx")) {
                wb = new XSSFWorkbook(fin);
            }
            else {
                System.out.println("Unsupported file type.");
                System.exit(1);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        finally {
            if (fin != null) {
                fin.close();
            }
        }
        return wb;
    }

    /**
     * Find all of the formula in this workbook.
     */
    private HashMap<String, String> findAllFormulae() {
        HashMap<String, String> formulae = new HashMap<>();

        for (Sheet sheet : this.workbook) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellTypeEnum() == CellType.FORMULA) {
                        formulae.put(cell.getAddress().toString(), cell.getCellFormula());
                    }
                }
            }
        }
        return formulae;
    }


    /**
     * Evaluate a formula in a cell.
     *
     * @param   sheetnumber
     * @param   cellreference
     */
    public void evaluateFormula(final int sheetnumber, final String cellreference) {
        if (sheetnumber > this.nsheets) {
            System.exit(1);
        }
        FormulaEvaluator evaluator = this.workbook.getCreationHelper().createFormulaEvaluator();
        CellReference cellref = new CellReference(cellreference);
        Sheet sheet = this.workbook.getSheetAt(sheetnumber);
        Row row = sheet.getRow(cellref.getRow());
        Cell cell = row.getCell(cellref.getCol());

        CellValue val = evaluator.evaluate(cell);
    }

    /**
     * Evaluate all the cells in a Sheet.
     *
     * The caller is responsible for knowing where the result goes. It will probably be
     * a sheet somewhere.
     *
     * Note: this changes our world!
     *
     * @param   sheetnumber     the number of the sheet to get
     */
    public void evaluateAll(final int sheetnumber) {
        if (sheetnumber > this.nsheets) {
            System.exit(1);
        }

        FormulaEvaluator evaluator = this.workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
    }

    /**
     * @return  the number of sheets in this workbook
     */
    public int getNsheets() {
        return this.nsheets;
    }

    /**
     * Get the value from a cell.
     *
     * Returns a map with {cell reference : value}.
     *
     * Note: Not all cells will have a value. If the cell is blank or doesn't exist
     *       then the resulting map will have a null value. It is the caller's
     *       responsibility to check for null.
     *
     * @param   sheetnumber      an integer
     * @param   cellreferences   a string like "A3"
     * @return  map              a hash map like {cellreference : value | null}
     */
    public HashMap<String, Object> getCellValue(final int sheetnumber, final String[] cellreferences) {

        HashMap<String, Object> map = new HashMap<>();

        for (String cr : cellreferences) {
            Sheet sheet = this.workbook.getSheetAt(sheetnumber);
            CellReference cellref = new CellReference(cr);
            Row row = sheet.getRow(cellref.getRow());
            Cell cell = row.getCell(cellref.getCol());

            String addr = cell.getAddress().toString();

            switch (cell.getCellTypeEnum()) {
                case FORMULA:
                    map.put(addr, cell.getNumericCellValue());
                    break;
                case BLANK:
                    map.put(addr, cell.getRichStringCellValue());
                    break;
                case STRING:
                    map.put(addr, cell.getStringCellValue());
                    break;
                case BOOLEAN:
                    map.put(addr, cell.getBooleanCellValue());
                    break;
                case NUMERIC:
                    map.put(addr, cell.getNumericCellValue());
                    break;
                case ERROR:
                    map.put(addr, cell.getErrorCellValue());
                    break;
                case _NONE:
                    map.put(addr, cell.getRichStringCellValue());
                    break;
            }
        }
        return map;
    }

    public Boolean populate(final int sheetnumber, HashMap<String, Object> map) {

        Boolean success = false;

        Sheet sheet = this.workbook.getSheetAt(sheetnumber);

        for (HashMap.Entry<String, Object> item : map.entrySet()) {
            CellReference ref = new CellReference(item.getKey());
            Row row = sheet.getRow(ref.getRow());
            Cell cell = row.getCell(ref.getCol());

            switch (cell.getCellTypeEnum()) {
                case FORMULA:
                    cell.setCellFormula(item.getValue().toString());
                    success = true;
                    break;
                case BLANK:
                    break;
                case STRING:
                    cell.setCellValue((String) item.getValue());
                    success = true;
                    break;
                case BOOLEAN:
                    cell.setCellValue((Boolean) item.getValue());
                    success = true;
                    break;
                case NUMERIC:
                    cell.setCellValue((Double) item.getValue());
                    success = true;
                    break;
                case ERROR:
                    break;
                case _NONE:
                    break;
            }
        }
        return success;
    }

    private void testGetAllFormulae() {
        System.out.println("Testing getAllFormulae");

        /* Do the thing. */
        HashMap<String, String> formulae = findAllFormulae();

        assert formulae.get("A3").equals("0");
        assert formulae.get("B3").equals("SUM(B1:B2)");
    }

    private void testPopulate() {
        System.out.println("Testing populate");

        /* Make a map to populate the cells. */
        HashMap<String, Object> map = new HashMap<>();
        map.put("A1", "hello");
        map.put("A2", 42.0);
        map.put("A3", true);

        /* Do the thing. */
        assert populate(0, map) == true : "FAILED: populate";

        /* This is where we expect values. */
        String[] cellrefs = {"A1", "A2", "A3"};
        HashMap<String, Object> cells = this.getCellValue(0, cellrefs);

        assert cells.get("A1").equals("hello") : "FAILED: A1 is not hello";
        assert (Double) cells.get("A2") == 42.0 : "FAILED: A2 is not 42.0";
        assert (Double) cells.get("A3") == 0.0 : "FAILED: A3 is false";
    }

    public void testEvaluateAll() {
        System.out.println("Testing evaluateAll");

        /* Do the thing. */
        this.evaluateAll(0);

        /* This is where we expect values. */
        String[] cellrefs = {"B1", "B2", "B3"};

        HashMap<String, Object> cells = this.getCellValue(0, cellrefs);

        assert (Double) cells.get("B1") == 19.0;
        assert (Double) cells.get("B2") == 23.0;
        assert (Double) cells.get("B3") == 42.0;
    }

    public static void main(String[] args) throws IOException {

        /* Check the command line arguments. */
        if (args.length != 1) {
            System.out.format("args length %s", args.length);
            System.out.println("usage: java Main [excel filename]");
            System.exit(2);
        }

        String filename = args[0];

        XlReader xlreader = new XlReader(filename);

        //xlreader.findAllFormulae();
        //xlreader.evaluateFormula(0, "A3");

        //String[] cells = {"D5", "D7", "D10", "D14", "C16", "D22", "D24", "D26"};
        //HashMap vals = xlreader.getCellValue(2, cells);
        //vals.forEach((k, v) -> System.out.format("Cell: %s   Value: %s\n", k, v));

        xlreader.testGetAllFormulae();
        xlreader.testPopulate();
        xlreader.testEvaluateAll();
    }
}
