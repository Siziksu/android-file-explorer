package com.siziksu.explorer.presenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.siziksu.explorer.R;
import com.siziksu.explorer.common.Constants;
import com.siziksu.explorer.common.comparators.FileComparator;
import com.siziksu.explorer.common.files.FileUtils;
import com.siziksu.explorer.common.functions.Done;
import com.siziksu.explorer.common.functions.Fail;
import com.siziksu.explorer.common.functions.Success;
import com.siziksu.explorer.common.model.Folder;
import com.siziksu.explorer.common.model.State;
import com.siziksu.explorer.domain.IGetFilesRequest;
import com.siziksu.explorer.ui.adapter.FilesAdapter;
import com.siziksu.explorer.ui.adapter.HeaderAdapter;
import com.siziksu.explorer.ui.view.SmoothLayoutManager;
import com.siziksu.explorer.ui.view.DividerDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilesPresenter implements IFilesPresenter {

    private static final String ROOT_PATH = "/";
    private static final String EXTRAS_STATE = "state";

    private IFilesView view;

    private File directory;
    private List<Folder> folders;
    private List<File> files;

    private HeaderAdapter headerAdapter;
    private FilesAdapter filesAdapter;

    private boolean showHidden;
    private boolean showSymLinks;
    private IGetFilesRequest getFilesData;

    private RecyclerView headerRecyclerView;

    public FilesPresenter() {
        directory = new File(ROOT_PATH);
        showHidden = true;
        showSymLinks = true;
        files = new ArrayList<>();
        folders = new ArrayList<>();
        folders.add(new Folder(ROOT_PATH, directory));
    }

    @Override
    public void register(IFilesView view) {
        this.view = view;
    }

    @Override
    public void unregister() {
        view = null;
    }

    @Override
    public FilesPresenter setGetFilesRequest(IGetFilesRequest getFilesData) {
        this.getFilesData = getFilesData;
        return this;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        State state = new State();
        state.setDirectory(directory);
        state.setFiles(files);
        outState.putParcelable(EXTRAS_STATE, state);
    }

    @Override
    public void fileClicked(int position) {
        File newFile = files.get(position);
        if (newFile != null) {
            if (newFile.isDirectory()) {
                directory = newFile;
                getFiles();
                folders.add(new Folder(directory.getName(), directory));
                headerAdapter.notifyDataSetChanged();
                scrollHeaderToEnd();
            } else {
                openFile(newFile);
            }
        }
    }

    @Override
    public void folderClicked(int position) {
        directory = folders.get(position).getFolder();
        for (int i = folders.size() - 1; i > position; i--) {
            folders.remove(i);
        }
        getFiles();
    }

    @Override
    public void getFiles() {
        getFilesData.init(directory, showHidden, showSymLinks)
                    .getFiles(
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
                                        filesAdapter.notifyDataSetChanged();
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
    public void getFiles(Bundle savedInstanceState) {
        if (view != null) {
            State state = getState(savedInstanceState);
            if (state != null) {
                directory = state.getDirectory();
                if (!state.getFiles().isEmpty()) {
                    files.addAll(state.getFiles());
                    filesAdapter.notifyDataSetChanged();
                    view.folderEmpty(false);
                } else {
                    view.folderEmpty(true);
                }
                scrollHeaderToEnd();
            }
        }
    }

    @Override
    public void setHeader(Activity activity, int id, HeaderAdapter.OnAdapterListener listener) {
        headerRecyclerView = (RecyclerView) activity.findViewById(id);
        headerRecyclerView.setLayoutManager(new SmoothLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        headerAdapter = new HeaderAdapter(activity, folders, listener);
        headerRecyclerView.setAdapter(headerAdapter);
    }

    @Override
    public void setRecyclerView(Activity activity, int id, FilesAdapter.OnAdapterListener listener) {
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(id);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        filesAdapter = new FilesAdapter(activity, files, listener);
        recyclerView.setAdapter(filesAdapter);
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerDecoration(ContextCompat.getDrawable(activity, R.drawable.recycler_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public boolean onBackPressed() {
        if (FileUtils.isRoot(directory)) {
            return true;
        } else {
            if (directory.getParentFile() != null) {
                directory = directory.getParentFile();
                getFiles();
                folders.remove(folders.size() - 1);
                headerAdapter.notifyDataSetChanged();
                scrollHeaderToEnd();
            }
            return false;
        }
    }

    private void scrollHeaderToEnd() {
        headerRecyclerView.smoothScrollToPosition(folders.size() - 1);
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