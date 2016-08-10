package com.siziksu.explorer.presenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;

import com.siziksu.explorer.R;
import com.siziksu.explorer.common.Constants;
import com.siziksu.explorer.common.comparators.FileComparator;
import com.siziksu.explorer.common.files.FileUtils;
import com.siziksu.explorer.common.functions.Done;
import com.siziksu.explorer.common.functions.Fail;
import com.siziksu.explorer.common.functions.Success;
import com.siziksu.explorer.common.model.State;
import com.siziksu.explorer.domain.GetFilesRequest;
import com.siziksu.explorer.ui.adapter.FilesAdapter;
import com.siziksu.explorer.ui.view.DividerDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainPresenterImpl implements MainPresenter {

    private static final String ROOT_PATH = "/";
    private static final String EXTRAS_STATE = "state";

    private MainView view;

    private File directory;
    private List<File> files;

    private FilesAdapter adapter;

    private boolean showHidden;
    private boolean showSymLinks;

    public MainPresenterImpl() {
        directory = new File(ROOT_PATH);
        showHidden = true;
        showSymLinks = true;
        files = new ArrayList<>();
    }

    @Override
    public void register(MainView view) {
        this.view = view;
    }

    @Override
    public void unregister() {
        view = null;
    }

    @Override
    public void fileClicked(int position) {
        File newFile = files.get(position);
        if (newFile != null) {
            if (newFile.isDirectory()) {
                directory = newFile;
                getFiles();
            } else {
                openFile(newFile);
            }
        }
    }

    @Override
    public void getFiles() {
        new GetFilesRequest(directory, showHidden, showSymLinks).getFiles(
                new Success<List<File>>() {

                    @Override
                    public void success(List<File> response) {
                        if (view != null) {
                            files.clear();
                            if (response != null) {
                                if (!response.isEmpty()) {
                                    files.addAll(response);
                                    Collections.sort(files, new FileComparator());
                                    view.folderEmpty(false);
                                } else {
                                    view.folderEmpty(true);
                                }
                            }
                            setPath();
                            adapter.notifyDataSetChanged();
                        }
                    }
                },
                new Fail() {
                    @Override
                    public void fail(Throwable throwable) {

                    }
                }, new Done() {
                    @Override
                    public void done() {

                    }
                }
        );
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        State state = new State();
        state.setDirectory(directory);
        state.setFiles(files);
        outState.putParcelable(EXTRAS_STATE, state);
    }

    @Override
    public void getFiles(Bundle savedInstanceState) {
        if (view != null) {
            State state = getState(savedInstanceState);
            if (state != null) {
                directory = state.getDirectory();
                if (!state.getFiles().isEmpty()) {
                    files.addAll(state.getFiles());
                    adapter.notifyDataSetChanged();
                    view.folderEmpty(false);
                } else {
                    view.folderEmpty(true);
                }
                setPath();
            }
        }
    }

    @Override
    public void setRecyclerView(Activity activity, int id, FilesAdapter.OnAdapterListener listener) {
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(id);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        adapter = new FilesAdapter(activity, files, listener);
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerDecoration(ContextCompat.getDrawable(activity, R.drawable.recycler_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void fullScroll(final HorizontalScrollView view) {
        view.post(new Runnable() {

            @Override
            public void run() {
                view.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        if (FileUtils.isRoot(directory)) {
            return true;
        } else {
            if (directory.getParentFile() != null) {
                directory = directory.getParentFile();
                getFiles();
            }
            return false;
        }
    }

    private void setPath() {
        if (!FileUtils.isRoot(directory)) {
            view.setPath(directory.getAbsolutePath() + "/");
        } else {
            view.setPath(directory.getAbsolutePath());
        }
    }

    private State getState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(EXTRAS_STATE)) {
            return savedInstanceState.getParcelable(EXTRAS_STATE);
        }
        return null;
    }

    private void openFile(File newFile) {
        if (FileUtils.tryOpenWithDefaultMimeType(view.getActivity(), newFile)) {
            return;
        }
        if (FileUtils.tryOpenAsPlainText(view.getActivity(), newFile)) {
            return;
        }
        Log.d(Constants.TAG, "No Activity found to handle the Intent");
    }
}
