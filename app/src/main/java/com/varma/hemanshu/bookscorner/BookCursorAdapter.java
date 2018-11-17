package com.varma.hemanshu.bookscorner;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.varma.hemanshu.bookscorner.data.BookContract;
import com.varma.hemanshu.bookscorner.data.BookContract.BookEntry;

/**
 * {@link BookCursorAdapter} is an adapter for a list view
 * that uses a {@link Cursor} of book data as its data source. This adapter knows
 * how to create list items for each row of book data in the {@link Cursor}.
 */
public class BookCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = BookCursorAdapter.class.getSimpleName();
    TextView bookName;
    TextView bookPrice;
    TextView bookQuantity;
    Button saleButton;

    /**
     * Constructs a new {@link BookCursorAdapter}.
     *
     * @param context The context
     * @param cursor  The cursor from which to get the data.
     */
    public BookCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Log.i(LOG_TAG,context.getString(R.string.inside_newView));
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the book data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current book can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {

        // Find individual views that to modify in the list item layout
        bookName = view.findViewById(R.id.title_book_name);
        bookPrice = view.findViewById(R.id.book_price);
        bookQuantity = view.findViewById(R.id.book_quantity);
        saleButton = view.findViewById(R.id.sale_btn);

        // Read the book attributes from the Cursor for the current book
        final String name = cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_NAME));
        final int price = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_PRICE));
        // Converting the price obtained from db to String.
        // This conversion will let Text displayed on TextView
        String priceString = String.valueOf(price);
        final int quantityInt = cursor.getInt(cursor.getColumnIndex(BookEntry.COLUMN_BOOK_QUANTITY));
        String quantityString = String.valueOf(quantityInt);

        // column number of "_ID"
        int idColIndex = cursor.getColumnIndex(BookEntry._ID);

        // Read the book attributes from the Cursor for the current book for "Sale" button
        final long idVal = Integer.parseInt(cursor.getString(idColIndex));

        // Update the TextViews with the attributes for the current book
        bookName.setText(name);
        bookPrice.setText(priceString);
        bookQuantity.setText(quantityString);

        /*
         * Each list view item will have a "Sale" button
         * This "Sale" button has OnClickListener which will decrease the product quantity by one at a time.
         * Update is only carried out if quantity is greater than MIN_LIMIT.
         */
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri newUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, idVal);

                int quantity = quantityInt - BookContract.ONE;
                if (quantity >= BookContract.MIN_LIMIT) {
                    ContentValues values = new ContentValues();
                    values.put(BookEntry.COLUMN_BOOK_QUANTITY, quantity);
                    context.getContentResolver().update(newUri, values, null, null);
                    Log.i(LOG_TAG, context.getString(R.string.item_update_success));
                } else {
                    Toast.makeText(context, context.getString(R.string.sell_btn_toast), Toast.LENGTH_SHORT).show();
                    Log.e(LOG_TAG, context.getString(R.string.item_update_fail));
                }
            }
        });

        Log.i(LOG_TAG,context.getString(R.string.inside_bindView));
    }
}
