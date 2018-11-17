package com.varma.hemanshu.bookscorner;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.varma.hemanshu.bookscorner.data.BookContract.BookEntry;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    @BindView(R.id.list_view)
    ListView listView;
    @BindView(R.id.empty_view)
    View emptyView;
    @BindView(R.id.fab)
    FloatingActionButton floatingActionButton;

    /**
     * Identifier for the book data loader
     */
    private static final int BOOK_LOADER = 0;

    /**
     * Adapter for the ListView
     */
    private BookCursorAdapter mBookCursorAdapter;

    /**
     * Tag for Logging
     **/
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // FAB for Redirecting to Add Book Activity.
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BooksEditorActivity.class);
                startActivity(intent);
            }
        });

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        listView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of book data in the Cursor.
        // There is no book data yet (until the loader finishes) so pass in null for the Cursor.
        mBookCursorAdapter = new BookCursorAdapter(this, null);
        listView.setAdapter(mBookCursorAdapter);

        // Setup the item click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(MainActivity.this, BooksEditorActivity.class);

                // Form the content URI that represents the specific book that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link BookEntry #CONTENT_URI}.
                // For example, the URI would be "content://com.varma.hemanshu.bookscorner/books/2"
                // if the book with ID 2 was clicked on.
                Uri currentBookUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, id);
                i.setData(currentBookUri);
                startActivity(i);
            }
        });
        getSupportLoaderManager().initLoader(BOOK_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all_records:
                deleteAllPets();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to delete all pets in the database.
     */
    private void deleteAllPets() {
        // Deletes the rows that match the selection criteria
        int rowsDeleted = getContentResolver().delete(BookEntry.CONTENT_URI, null, null);

        if (rowsDeleted == 0) {
            // If the value of rowsDeleted is 0, then there was problem with deleting rows
            // or no rows match the selection criteria.
            Toast.makeText(this, R.string.error_while_deleting_books,
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the deletion was successful and we can display a toast.
            Toast.makeText(this, R.string.all_books_deleted,
                    Toast.LENGTH_SHORT).show();
        }
        Log.v(LOG_TAG, getString(R.string.deleted_rows) + rowsDeleted);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {BookEntry._ID,
                BookEntry.COLUMN_BOOK_NAME,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY};

        Log.i(LOG_TAG, getString(R.string.inside_onCreateLoader));

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this, BookEntry.CONTENT_URI, projection,
                null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        Log.i(LOG_TAG, getString(R.string.inside_onLoadFinished));
        // Update {@link BookCursorAdapter} with this new cursor containing updated pet data
        mBookCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        Log.i(LOG_TAG, getString(R.string.inside_onLoaderReset));
        // Callback called when the data needs to be deleted
        mBookCursorAdapter.swapCursor(null);
    }
}
