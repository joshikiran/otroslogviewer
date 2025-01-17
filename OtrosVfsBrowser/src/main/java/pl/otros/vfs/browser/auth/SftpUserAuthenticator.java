/*
 * Copyright 2013 Krzysztof Otrebski (otros.systems@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.otros.vfs.browser.auth;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.IdentityProvider;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import pl.otros.vfs.browser.i18n.Messages;
import pl.otros.vfs.browser.util.PageantIdentityRepositoryFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.OptionalInt;

public class SftpUserAuthenticator extends UserPassUserAuthenticator {

  private JTextField sshKeyFileField;
  private static JFileChooser chooser;

  public SftpUserAuthenticator(AuthStore authStore, String url, FileSystemOptions fileSystemOptions) {
    super(authStore, url, fileSystemOptions);

  }

  @Override
  protected void getAuthenticationData(UserAuthenticationData authenticationData) {
    authenticationData.setData(UserAuthenticationData.USERNAME, nameTf.getSelectedItem().toString().toCharArray());

    if (StringUtils.isNotBlank(sshKeyFileField.getText())) {
      //use SSH KEY
      authenticationData.setData(UserAuthenticationDataWrapper.SSH_KEY, sshKeyFileField.getText().trim().toCharArray());
      IdentityProvider sshKeyAuth;
      if (passTx.getPassword() != null && passTx.getPassword().length > 0) {
        //SSH KEY secured with password
        String stringPass = new String(passTx.getPassword());
        sshKeyAuth = new IdentityInfo(new File(sshKeyFileField.getText()), stringPass.getBytes());
      } else {
        sshKeyAuth = new IdentityInfo(new File(sshKeyFileField.getText()));
      }
      SftpFileSystemConfigBuilder.getInstance().setIdentityProvider(getFileSystemOptions(), sshKeyAuth);
    } else {
      authenticationData.setData(UserAuthenticationData.PASSWORD, passTx.getPassword());
    }

  }

  @Override
  protected JPanel getOptionsPanel() {
    if (sshKeyFileField == null) {
      sshKeyFileField = new JTextField(15);
    }
    if (chooser == null) {
      chooser = new JFileChooser();
    }
    JPanel panel = super.getOptionsPanel();
    panel.add(new JLabel(Messages.getMessage("authenticator.sshKeyFile")));

    panel.add(sshKeyFileField, "grow");
    JButton browseButton = new JButton(Messages.getMessage("authenticator.browse"));
    browseButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(Messages.getMessage("authenticator.selectSshKey"));
        int showOpenDialog = chooser.showOpenDialog(null);
        if (showOpenDialog == JFileChooser.APPROVE_OPTION) {
          sshKeyFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
      }
    });
    panel.add(browseButton, "wrap");

    OptionalInt pageantActive = this.isPageantActive();
    String pageantInfo;
    if (pageantActive.isPresent()) {
      pageantInfo = Messages.getMessage("authenticator.pageantActiveCount", pageantActive.getAsInt());
    } else {
      pageantInfo = Messages.getMessage("authenticator.pageantInactive");
    }

    panel.add(new JLabel(pageantInfo), "span");

    return panel;
  }

  private OptionalInt isPageantActive() {
    return PageantIdentityRepositoryFactory.getIdentitiesCount();
  }


  @Override
  protected void userSelectedHook(UserAuthenticationData userAuthenticationData) {
    if (userAuthenticationData != null) {
      char[] sshKeyPath = userAuthenticationData.getData(UserAuthenticationDataWrapper.SSH_KEY);
      String path = "";
      if (sshKeyPath != null && sshKeyPath.length > 0) {
        path = new String(sshKeyPath);
      }
      sshKeyFileField.setText(path);
    }
  }


}
