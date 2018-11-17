package com.varma.hemanshu.bookscorner;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.varma.hemanshu.bookscorner.data.BookContract;
import com.varma.hemanshu.bookscorner.data.BookContract.BookEntry;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BooksEditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Find all relevant views that we will need to read user input from
     **/
    @BindView(R.id.book_name_et)
    EditText mBookName;
    @BindView(R.id.book_price_et)
    EditText mBookPrice;
    @BindView(R.id.decrement_btn)
    Button decrementBTN;
    @BindView(R.id.book_quantity_tv)
    TextView mBookQuantity;
    @BindView(R.id.increment_btn)
    Button incrementBTN;
    @BindView(R.id.supplier_name_et)
    EditText mSupplierName;
    @BindView(R.id.supplier_phone_et)
    EditText mSupplierPhone;
    @BindView(R.id.contact_supplier)
    Button mContactSupplier;

    /**
     * Content URI for the existing book (null if it's a new book)
     */
    private Uri mBookUri;

    /**
     * Quantity variable for getting the count from local db
     **/
    private int quantity;

    /**
     * Tag for Logging
     **/
    private static final String LOG_TAG = BooksEditorActivity.class.getSimpleName();

    /**
     * Identifier for the book data loader
     */
    private static final int EXISTING_LOADER = 0;

    /**
     * Supplier contact number will be save in supplierContact variable
     **/
    private String supplierPhone;

    /**
     * Boolean flag that keeps track of whether the book has been edited (true) or not (false)
     */
    private boolean mBookHasChanged = false;

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the bookHasChanged boolean to true.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mBookHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_books_editor);

        // Using ButterKnife Binding for Linking Views.
        ButterKnife.bind(this);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new book or editing an existing one.
        Intent i = getIntent();
        mBookUri = i.getData();

        // If the intent DOES NOT contain a book content URI, then we know that we are
        // creating a new book.
        if (mBookUri == null) {
            // This is a new book, so change the app bar to say "Add a Book"
            setTitle(getString(R.string.add_book));

            // Invalidate the options menu, so the "Delete" and "Contact Supplier" menu option can be hidden.
            // (It doesn't make sense to delete a book or contact supplier that hasn't been created yet.)
            invalidateOptionsMenu();

            // Hiding the Button in Add Book as the supplier number hasn't been added yet
            mContactSupplier.setVisibility(View.GONE);

        } else {
            // Otherwise this is an existing book, so change app bar to say "Edit Book"
            setTitle(getString(R.string.edit_book));

            // OnClickListener for Call Button to call Supplier via an DIAL Intent
            mContactSupplier.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Parsing uri to supplier's phone number.
                    Uri phone = Uri.parse("tel:" + supplierPhone);

                    // Intent for Dialing
                    Intent phoneIntent = new Intent(Intent.ACTION_DIAL, phone);
                    // Verify that the intent will resolve to an activity
                    if (phoneIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(phoneIntent);
                    }
                }
            });

            // Initializing Loader Manager Callback for Activity
            getLoaderManager().initLoader(EXISTING_LOADER, null, this);
        }

        // OnClickListener for decrementing quantity by one.
        decrementBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity <= BookContract.MIN_LIMIT) {
                    Toast.makeText(BooksEditorActivity.this,
                            getString(R.string.decrement_quantity),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity -= BookContract.ONE;
                    mBookQuantity.setText(String.valueOf(quantity));
                }
            }
        });

        // OnClickListener for incrementing quantity by one.
        incrementBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity >= BookContract.MAX_LIMIT) {
                    Toast.makeText(BooksEditorActivity.this,
                            getString(R.string.increment_quantity),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity += BookContract.ONE;
                    mBookQuantity.setText(String.valueOf(quantity));
                }
            }
        });

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mBookName.setOnTouchListener(mTouchListener);
        mBookPrice.setOnTouchListener(mTouchListener);
        incrementBTN.setOnTouchListener(mTouchListener);
        decrementBTN.setOnTouchListener(mTouchListener);
        mSupplierName.setOnTouchListener(mTouchListener);
        mSupplierPhone.setOnTouchListener(mTouchListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_books_editor_menu, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mBookUri == null) {
            MenuItem deleteMenu = menu.findItem(R.id.delete_single_record);
            deleteMenu.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.save:
                saveBook();
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.delete_single_record:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                if (!mBookHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(BooksEditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {BookEntry._ID,
                BookEntry.COLUMN_BOOK_NAME,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY,
                BookEntry.COLUMN_BOOK_SUPPLIER_NAME,
                BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NO};

        Log.i(LOG_TAG, getString(R.string.inside_onCreateLoader));
        return new CursorLoader(this, mBookUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_NAME));
            int priceInt = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE));
            String priceString = String.valueOf(priceInt);
            int quantityInt = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY));
            String quantityString = String.valueOf(quantityInt);

            // Setting quantity when item is retrieved from list
            quantity = Integer.parseInt(quantityString);

            String supplierName = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_SUPPLIER_NAME));
            supplierPhone = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NO));
            // Setting Text onto Views
            mBookName.setText(name);
            mBookPrice.setText(priceString);
            mBookQuantity.setText(quantityString);
            mSupplierName.setText(supplierName);
            mSupplierPhone.setText(supplierPhone);

            Log.i(LOG_TAG, getString(R.string.inside_onLoadFinished));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(LOG_TAG, getString(R.string.inside_onLoaderReset));
        mBookName.setText(getString(R.string.book_name_hint));
        mBookPrice.setText(getString(R.string.price_hint));
        mBookQuantity.setText(getString(R.string.quantity_int));
        mSupplierName.setText(getString(R.string.supplier_hint));
        mSupplierPhone.setText(getString(R.string.supplier_phone_hint));
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the Book hasn't changed, continue with handling back button press
        if (!mBookHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    private void saveBook() {
        String nameString = mBookName.getText().toString().trim();
        String priceString = mBookPrice.getText().toString().trim();
        int priceInt = BookContract.MIN_LIMIT;
        String supplierNameString = mSupplierName.getText().toString().trim();
        String supplierPhoneString = mSupplierPhone.getText().toString().trim();

        if (mBookUri == null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString)
                && TextUtils.isEmpty(supplierPhoneString)) {
            Toast.makeText(this, getString(R.string.empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        priceInt = Integer.parseInt(priceString);

        ContentValues values = new ContentValues();
        values.put(BookEntry.COLUMN_BOOK_NAME, nameString);
        values.put(BookEntry.COLUMN_BOOK_PRICE, priceInt);
        values.put(BookEntry.COLUMN_BOOK_QUANTITY, quantity);
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_NAME, supplierNameString);
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NO, supplierPhoneString);

        Log.i(LOG_TAG, getString(R.string.inside_saveBook));
        if (mBookUri == null) {
            Uri newUri = getContentResolver().insert(BookEntry.CONTENT_URI, values);
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.insert_book_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.insert_book_success),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mBookUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_successful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete_single_record, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the Book.
                deleteBook();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the Book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the Book in the database.
     */
    private void deleteBook() {
        // Only perform the delete if this is an existing Book.
        if (mBookUri != null) {
            // Call the ContentResolver to delete the Book at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentBookUri
            // content URI already identifies the Book that we want.
            int rowsDeleted = getContentResolver().delete(mBookUri, null, null);
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_book_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_book_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        Log.i(LOG_TAG, getString(R.string.inside_deleteBook));
        // Close the activity
        finish();
    }
}
