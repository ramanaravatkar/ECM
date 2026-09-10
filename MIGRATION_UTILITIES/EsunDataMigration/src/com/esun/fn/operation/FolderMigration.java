

package com.esun.fn.operation;

import com.filenet.api.core.ObjectStore;

public interface FolderMigration {

    void migrate(ObjectStore os, String localRoot, String targetFolder);

}
