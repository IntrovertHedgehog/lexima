package vocabletrainer.heinecke.aron.vocabletrainer.eximport;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.Exporter;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Function;

/**
 * Export ViewModel containinig relevant data & handles background tasking
 */
public class ExportViewModel extends ViewModel {
    private final static String TAG = "ExportViewModel";
    private int exportListAmount; // TODO: temporary until selector has own viewmodel
    private final MutableLiveData<Integer> progressExport;
    private final MutableLiveData<Boolean> exporting;
    private final MutableLiveData<Boolean> cancelExport;
    private final MutableLiveData<Boolean> exportFinished;
    private final MutableLiveData<String> exception;
    private final Observer<Boolean> observeCancel;
    private AsyncTask task;

    public ExportViewModel() {
        exportListAmount = 0;
        progressExport = new MutableLiveData<>();
        exporting = new MutableLiveData<>();
        cancelExport = new MutableLiveData<>();
        exportFinished = new MutableLiveData<>();
        exception = new MutableLiveData<>();
        cancelExport.setValue(false);
        exporting.setValue(false);
        progressExport.setValue(0);
        observeCancel = aBoolean -> {
            if(aBoolean != null && aBoolean && task != null && task.getStatus() == AsyncTask.Status.RUNNING){
                task.cancel(true);
            }
        };
        cancelExport.observeForever(observeCancel);
    }

    /**
     * Resets exception back to null (none)
     */
    public void resetException(){
        exception.setValue(null);
    }

    /**
     * Returns exception handle, for error reporting
     * @return
     */
    public LiveData<String> getExceptionHandle() {
        return exception;
    }

    /**
     * Get cancelExport handle
     * @return mutable LiveData handle for cancelling export
     */
    public MutableLiveData<Boolean> getCancelExportHandle() { return cancelExport; }


    /**
     * Returns exporting status handle
     * @return LiveData handle
     */
    public LiveData<Boolean> getExportingHandles(){
        return exporting;
    }

    /**
     * Returns progress export handle
     * @return LiveData handle
     */
    public LiveData<Integer> getProgressExportHandle() {
        return progressExport;
    }

    /**
     * Returns size of export
     * @return amount VLists
     */
    public int getExportSize(){
        return exportListAmount;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelExport.removeObserver(observeCancel);
    }

    public LiveData<Boolean> getExportFinishedHandle() {
        return exportFinished;
    }

    /**
     * Reset exportFinished value
     */
    public void resetExportFinished(){
        exportFinished.setValue(false);
    }

    /**
     * Run Export task
     * @param context
     */
    public void runExport(Context context, ExportFragment.ExportStorage exportStorage){
        if(task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.w(TAG,"preventing second running export");
            return;
        }
        //post export
        Function<Void,String> callback = param -> {
            exporting.setValue(false);
            exportFinished.setValue(true);
            exportListAmount = 0;
            return null;
        };
        Function<Void,String> callbackCancel = param -> {
            exporting.setValue(false);
            exportListAmount = 0;
            cancelExport.setValue(false);
            return null;
        };

        Exporter exporter = new Exporter(exportStorage,progressExport,callback,callbackCancel,context,
                exception);
        this.exportListAmount = exportStorage.lists.size();
        this.progressExport.setValue(0); // don't start on max on redo
        this.exporting.setValue(true);
        resetException();
        task = exporter.execute(0); // 0 is just to pass something
    }

}
