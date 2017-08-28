package vocabletrainer.heinecke.aron.vocabletrainer.lib.Import;

import java.util.List;

import vocabletrainer.heinecke.aron.vocabletrainer.lib.Storage.Entry;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Storage.Table;

import static vocabletrainer.heinecke.aron.vocabletrainer.lib.Database.ID_RESERVED_SKIP;

/**
 * Preview parser handler also counting parsing metadata stats<br>
 * Limiting amount of entries parsed per list
 */
public class PreviewParser implements ImportHandler {
    private final static int PARSE_LIMIT = 5;
    private final List<Entry> list;
    private final Table tbl = null;
    private int parsed_limiter = 0;
    private int tblCount = 0;
    private int rows = 0;

    public PreviewParser(List<Entry> list) {
        this.list = list;
    }

    @Override
    public void newTable(String name, String columnA, String columnB) {
        list.add(new Entry(columnA, columnB, name, ID_RESERVED_SKIP, tbl, -2L));
        parsed_limiter = 0;
        tblCount++;
        rows++;
    }

    @Override
    public void newEntry(String A, String B, String Tipp) {
        rows++;
        if (parsed_limiter < PARSE_LIMIT) {
            list.add(new Entry(A, B, Tipp, null, -2L));
            parsed_limiter++;
        }
    }

    /**
     * Returns amount of rows detected
     *
     * @return
     */
    public int getAmountRows() {
        return rows;
    }

    @Override
    public void finish() {

    }

    @Override
    public void start() {

    }

    /**
     * Is parsed list raw data without list metadata
     *
     * @return
     */
    public boolean isRawData() {
        return tblCount == 0;
    }

    /**
     * Is parsed list a multilist
     *
     * @return
     */
    public boolean isMultiList() {
        return tblCount > 1;
    }
}