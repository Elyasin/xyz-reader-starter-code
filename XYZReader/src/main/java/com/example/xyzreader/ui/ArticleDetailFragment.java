package com.example.xyzreader.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private ImageView mPhoto;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;


    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private FloatingActionButton mFab;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null &&
                getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mFab = mRootView.findViewById(R.id.floating_action_button);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));

            }
        });

        mPhoto = mRootView.findViewById(R.id.photo);

        mCollapsingToolbarLayout = mRootView.findViewById(R.id.collapsing_toolbar_container);
        //TODO Answer on StackOverflow
        mCollapsingToolbarLayout.setExpandedTitleTypeface(Typeface.create(mCollapsingToolbarLayout.getExpandedTitleTypeface(), Typeface.BOLD));
        mCollapsingToolbarLayout.setCollapsedTitleTypeface(Typeface.create(mCollapsingToolbarLayout.getExpandedTitleTypeface(), Typeface.BOLD));
        mCollapsingToolbarLayout.setCollapsedTitleTextColor(getResources().getColor(android.R.color.white));

        AppCompatActivity appCompatActivity = ((AppCompatActivity) getActivity());
        Toolbar toolbar = mRootView.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        appCompatActivity.setSupportActionBar(toolbar);
        ActionBar actionBar = appCompatActivity.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        bindViews();

        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView byLineView = mRootView.findViewById(R.id.article_byline);
        TextView bodyView = mRootView.findViewById(R.id.article_body);
        final LinearLayout titleBylineView = mRootView.findViewById(R.id.linear_layout_title_container);

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            String titleString = mCursor.getString(ArticleLoader.Query.TITLE);
            mCollapsingToolbarLayout.setTitle(titleString);

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                byLineView.setText(
                        Html.fromHtml(
                                DateUtils.getRelativeTimeSpanString(
                                        publishedDate.getTime(),
                                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                        DateUtils.FORMAT_ABBREV_ALL).toString()
                                        + " by "
                                        + mCursor.getString(ArticleLoader.Query.AUTHOR))
                );

            } else {
                // If date is before 1902, just show the string
                byLineView.setText(
                        Html.fromHtml(
                                outputFormat.format(publishedDate) + " by "
                                        + mCursor.getString(ArticleLoader.Query.AUTHOR))
                );

            }

            bodyView.setText(Html.fromHtml(
                    mCursor.getString(ArticleLoader.Query.BODY)
                            .replaceAll("(\r\n|\n)", "<br />")));

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            mPhoto.setImageBitmap(imageContainer.getBitmap());
                            Palette p = new Palette.Builder(imageContainer.getBitmap())
                                    .maximumColorCount(12)
                                    .generate();
                            int darkMutedColor = p.getDarkMutedColor(Color.BLACK);
                            mCollapsingToolbarLayout.setContentScrimColor(darkMutedColor);
                            titleBylineView.setBackgroundColor(darkMutedColor);
                            mFab.setBackgroundTintList(ColorStateList.valueOf(darkMutedColor));
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Log.e(TAG, "Image could not be loaded.");
                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            bodyView.setText("N/A");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
