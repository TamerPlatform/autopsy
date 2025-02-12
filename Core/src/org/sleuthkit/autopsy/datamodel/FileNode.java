/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 - 2013 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.datamodel;

import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.actions.AddContentTagAction;
import org.sleuthkit.autopsy.coreutils.ContextMenuExtensionPoint;
import org.sleuthkit.autopsy.coreutils.ImageUtils;
import org.sleuthkit.autopsy.directorytree.ExternalViewerAction;
import org.sleuthkit.autopsy.directorytree.ExtractAction;
import org.sleuthkit.autopsy.directorytree.HashSearchAction;
import org.sleuthkit.autopsy.directorytree.NewWindowViewAction;
import org.sleuthkit.autopsy.directorytree.ViewContextAction;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.TskData.TSK_DB_FILES_TYPE_ENUM;
import org.sleuthkit.datamodel.TskData.TSK_FS_NAME_FLAG_ENUM;

/**
 * This class is used to represent the "Node" for the file. It may have derived
 * files children.
 */
public class FileNode extends AbstractFsContentNode<AbstractFile> {

    /**
     * @param file underlying Content
     */
    public FileNode(AbstractFile file) {
        this(file, true);

        setIcon(file);
    }

    public FileNode(AbstractFile file, boolean directoryBrowseMode) {
        super(file, directoryBrowseMode);

        setIcon(file);
    }

    private void setIcon(AbstractFile file) {
        // set name, display name, and icon
        if (file.isDirNameFlagSet(TSK_FS_NAME_FLAG_ENUM.UNALLOC)) {
            if (file.getType().equals(TSK_DB_FILES_TYPE_ENUM.CARVED)) {
                this.setIconBaseWithExtension("org/sleuthkit/autopsy/images/carved-file-icon-16.png"); //NON-NLS
            } else {
                this.setIconBaseWithExtension("org/sleuthkit/autopsy/images/file-icon-deleted.png"); //NON-NLS
            }
        } else {
            this.setIconBaseWithExtension(getIconForFileType(file));
        }
    }

    /**
     * Right click action for this node
     *
     * @param popup
     *
     * @return
     */
    @Override
    public Action[] getActions(boolean popup) {
        List<Action> actionsList = new ArrayList<>();
        if (!this.getDirectoryBrowseMode()) {
            actionsList.add(new ViewContextAction(NbBundle.getMessage(FileNode.class, "FileNode.viewFileInDir.text"), this));
            actionsList.add(null); // creates a menu separator
        }
        actionsList.add(new NewWindowViewAction(
                NbBundle.getMessage(FileNode.class, "FileNode.getActions.viewInNewWin.text"), this));
        actionsList.add(new ExternalViewerAction(
                NbBundle.getMessage(FileNode.class, "FileNode.getActions.openInExtViewer.text"), this));
        actionsList.add(null); // creates a menu separator
        actionsList.add(ExtractAction.getInstance());
        actionsList.add(new HashSearchAction(
                NbBundle.getMessage(FileNode.class, "FileNode.getActions.searchFilesSameMD5.text"), this));
        actionsList.add(null); // creates a menu separator        
        actionsList.add(AddContentTagAction.getInstance());
        actionsList.addAll(ContextMenuExtensionPoint.getActions());
        return actionsList.toArray(new Action[0]);
    }

    @Override
    public <T> T accept(ContentNodeVisitor< T> v) {
        return v.visit(this);
    }

    @Override
    public <T> T accept(DisplayableItemNodeVisitor< T> v) {
        return v.visit(this);
    }

    // Given a file, returns the correct icon for said
    // file based off it's extension
    static String getIconForFileType(AbstractFile file) {
        // Get the name, extension
        String ext = file.getNameExtension();

        if (StringUtils.isBlank(ext)) {
            return "org/sleuthkit/autopsy/images/file-icon.png"; //NON-NLS
        } else {
            ext = "." + ext;
        }

        if (ImageUtils.isImageThumbnailSupported(file)
                || FileTypeExtensions.getImageExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/image-file.png"; //NON-NLS
        }
        // Videos
        if (FileTypeExtensions.getVideoExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/video-file.png"; //NON-NLS
        }
        // Audio Files
        if (FileTypeExtensions.getAudioExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/audio-file.png"; //NON-NLS
        }
        // Documents
        if (FileTypeExtensions.getDocumentExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/doc-file.png"; //NON-NLS
        }
        // Executables / System Files
        if (FileTypeExtensions.getExecutableExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/exe-file.png"; //NON-NLS
        }
        // Text Files
        if (FileTypeExtensions.getTextExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/text-file.png"; //NON-NLS
        }
        // Web Files
        if (FileTypeExtensions.getWebExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/web-file.png"; //NON-NLS
        }
        // PDFs
        if (FileTypeExtensions.getPDFExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/pdf-file.png"; //NON-NLS
        }
        // Archives
        if (FileTypeExtensions.getArchiveExtensions().contains(ext)) {
            return "org/sleuthkit/autopsy/images/archive-file.png"; //NON-NLS
        }
        // Else return the default
        return "org/sleuthkit/autopsy/images/file-icon.png"; //NON-NLS
    }

    @Override
    public boolean isLeafTypeNode() {
        // This seems wrong, but it also seems that it is never called
        // because the visitor to figure out if there are children or 
        // not will check if it has children using the Content API
        return true;
    }

    /*
     * TODO (AUT-1849): Correct or remove peristent column reordering code
     *
     * Added to support this feature.
     */
//    @Override
//    public String getItemType() {
//        return "File"; //NON-NLS
//    }
}
