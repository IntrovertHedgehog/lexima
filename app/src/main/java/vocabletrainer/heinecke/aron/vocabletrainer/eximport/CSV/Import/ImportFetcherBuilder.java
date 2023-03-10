package vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.Import;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.MutableLiveData;

import vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.CSVCustomFormat;
import vocabletrainer.heinecke.aron.vocabletrainer.lib.Function;

/**
 * ImportFetcher builder<br>
 *     because ImportFetcher requires too many parameters
 */
public class ImportFetcherBuilder {
    private CSVCustomFormat format;
    private Uri source;
    private ImportHandler handler;
    private Context context;
    private ImportFetcher.MessageProvider messageProvider;
    private Function<Void,String> importCallback;
    private Function<Void,String> cancelCallback;
    private MutableLiveData<Integer> progressHandle;
    private boolean logErrors = true;

    public ImportFetcherBuilder setFormat(CSVCustomFormat format) {
        this.format = format;
        return this;
    }

    public ImportFetcherBuilder setSource(Uri source) {
        this.source = source;
        return this;
    }

    public ImportFetcherBuilder setHandler(ImportHandler handler) {
        this.handler = handler;
        return this;
    }

    public ImportFetcherBuilder setProgressHandle(MutableLiveData<Integer> progressHandle) {
        this.progressHandle = progressHandle;
        return this;
    }

    public ImportFetcherBuilder setMessageProvider(ImportFetcher.MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
        return this;
    }

    public ImportFetcherBuilder setContext(Context context) {
        this.context = context;
        return this;
    }

    public ImportFetcherBuilder setImportCallback(Function<Void,String> importCallback) {
        this.importCallback = importCallback;
        return this;
    }

    public ImportFetcherBuilder setCancelCallback(Function<Void,String> cancelCallback) {
        this.cancelCallback = cancelCallback;
        return this;
    }

    public ImportFetcherBuilder setLogErrors(boolean logErrors) {
        this.logErrors = logErrors;
        return this;
    }

    /**
     * Finalize builder, create ImportFetcher
     * @return ImportFetcher
     */
    public ImportFetcher createImportFetcher() {
        if(format == null || source == null || handler == null || progressHandle == null
                || messageProvider == null || importCallback == null)
            throw new IllegalArgumentException();
        return new ImportFetcher(format, source, handler, progressHandle,
                messageProvider,importCallback,logErrors,cancelCallback,context);
    }
}