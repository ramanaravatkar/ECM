package com.esun.fn.operation;

import java.io.File;

import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;

public interface DocumentMigration {

	void uploadDocument(ObjectStore os, Folder folder, File file);

}