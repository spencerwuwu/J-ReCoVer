// https://searchcode.com/api/result/64434673/

package de.fau.fsahoy.android.api15.fragments.profile;

/**
 * Fragment which displays basic information section of user profile
 * 
 * @author Sabine Schmidt
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import de.fau.fsahoy.android.api15.R;
import de.fau.fsahoy.android.api15.ServerPaths;
import de.fau.fsahoy.android.api15.converter.DownloadProfilePictureTask;
import de.fau.fsahoy.android.api15.converter.profile.ProfileUpdateEditContainer;
import de.fau.fsahoy.android.api15.converter.profile.ProfileUpdateEditContainerImpl;
import de.fau.fsahoy.android.api15.converter.profile.ProfileUpdateViewContainer;
import de.fau.fsahoy.android.api15.converter.profile.ProfileUpdateViewContainerImpl;
import de.fau.fsahoy.android.api15.db.ProfileManager;
import de.fau.fsahoy.android.api15.listener.Ahoy_DateButtonListener;
import de.fau.fsahoy.android.api15.listener.profile.Profile_CameraButtonClickListener;
import de.fau.fsahoy.android.api15.listener.profile.Profile_CancelListener;
import de.fau.fsahoy.android.api15.listener.profile.Profile_EditListener;
import de.fau.fsahoy.android.api15.listener.profile.Profile_PickerButtonClickListener;
import de.fau.fsahoy.android.api15.listener.profile.Profile_SaveListener;
import de.fau.fsahoy.android.api15.profile.ProfileField;
import de.fau.fsahoy.android.api15.profile.dto.Countries;
import de.fau.fsahoy.android.api15.profile.dto.Gender;
import de.fau.fsahoy.android.api15.profile.dto.Profile;
import de.fau.fsahoy.android.api15.profile.dto.RelationshipStatus;
import de.fau.fsahoy.android.api15.view.AhoyButton;
import de.fau.fsahoy.android.api15.view.AhoyEditText;
import de.fau.fsahoy.android.api15.view.AhoySpinner;

public class Profile_BasicInformationFragment extends Fragment {

    // Display fields
    private ImageView                        userPicture;
    private TextView                         firstName;
    private TextView                         lastName;
    private TextView                         dateOfBirth;
    private TextView                         nationality;
    private TextView                         gender;
    private TextView                         relationshipStatus;
    private TextView                         language;
    private TextView                         aboutMe;

    // Edit fields
    private ImageView                        editUserPicture;
    private AhoyEditText                     editFirstName;
    private AhoyEditText                     editLastName;
    private AhoyButton                       editDateOfBirth;
    private AhoySpinner                      editNationality;
    private AhoySpinner                      editGender;
    private AhoySpinner                      editRelationshipStatus;
    private AhoyEditText                     editLanguage;
    private AhoyEditText                     editAboutMe;

    // Buttons to switchView
    private Button                           editBasicInformation;
    private Button                           saveBasicInformation;
    private Button                           cancelBasicInformation;

    private ViewSwitcher                     switcherBasicInformation;

    private ArrayAdapter<Countries>          adapterNationality;
    private ArrayAdapter<Gender>             adapterGender;
    private ArrayAdapter<RelationshipStatus> adapterRelationshipStatus;

    private Button                           takePhoto;
    private Button                           pickPhoto;
    public static final int                  CAMERA_REQUEST = 1888;
    public static final int                  PICKED_IMAGE   = 1889;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_basic, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);

        final Profile profile = ProfileManager.get(getActivity()).getProfile();

        if (null != profile) {
            findViewById();
            setEditAdapters(profile);
            setListeners(profile);
            updateBasicInformationViewFieldsFromProfile(profile);
        } else {
            Log.d("[Profile_BasicInformationFragment#onActivityCreated]", "Profile is null. Going back to MainMenu");
            getActivity().finish();

        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == 0) {
            // Canceled
        }

        if (resultCode == -1) {
            if (requestCode == CAMERA_REQUEST) {
                onPhotoTaken();
            }
            if (requestCode == PICKED_IMAGE) {
                if (data != null) {
                    onPhotoPicked(data);
                }
            }
        }
    }

    private void setUserPicture(final Drawable image) {
        if (image != null) {
            editUserPicture.setImageDrawable(image);
        }
    }

    private void CopyAssets(final File filename) {

        InputStream in = null;
        OutputStream out = null;
        try {
            final FileInputStream fin = new FileInputStream(filename);
            in = fin;
            out = new FileOutputStream(Environment.getExternalStorageDirectory() + "/profile_image.jpg");
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (final Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    private void copyFile(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    protected void onPhotoPicked(final Intent data) {
        final Uri selectedImage = data.getData();

        final String[] selectionArgs = null;
        final String sortOrder = null;

        // CursorLoader cursorLoader = new CursorLoader(getActivity(),
        // selectedImage, projection, selection, selectionArgs, sortOrder);
        // Cursor cursor = cursorLoader.loadInBackground();

        // Deprecated, fix throws no classDef error since the function isn't
        // available on Android 2.3.3 yet.
        final Cursor cursor = getActivity().managedQuery(selectedImage, selectionArgs, sortOrder, selectionArgs, sortOrder);

        if (cursor == null) {
            return;
        }

        cursor.moveToFirst();

        final int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        final String filePath = cursor.getString(column_index); // from Gallery

        CopyAssets(new File(filePath));

        cursor.close();

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4; // reduce image size to factor 1/4
        final Bitmap finalImage = BitmapFactory.decodeFile(filePath, options);

        scaleAndSetImage(finalImage);

    }

    protected void onPhotoTaken() {
        final String path = Environment.getExternalStorageDirectory() + "/profile_image.jpg";

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4; // reduce image size to factor 1/4
        final Bitmap finalImage = BitmapFactory.decodeFile(path, options);

        scaleAndSetImage(finalImage);
    }

    private void scaleAndSetImage(Bitmap finalImage) {
        // Scaling doesn't work correctly
        // TODO: What is this here for?
        if (finalImage.getWidth() > 1000) {
            final double scaleFactor = 1.0 / (finalImage.getWidth() / 1000.0);
            final double height = (finalImage.getHeight() * scaleFactor);
            final int floorHeight = (int) (finalImage.getHeight() * scaleFactor);
            final int roundedHeight = ((height - floorHeight) > 0.5) ? floorHeight + 1 : floorHeight;
            // crashes when using roundedHeight
            finalImage = Bitmap.createScaledBitmap(finalImage, 1000, roundedHeight, true);
        }
        final Drawable image = new BitmapDrawable(getResources(), finalImage);
        setUserPicture(image);
    }

    private void findViewById() {
        // Display fields
        userPicture = (ImageView) getActivity().findViewById(R.id.profilePicture);
        firstName = (TextView) getActivity().findViewById(R.id.profileFirstName);
        lastName = (TextView) getActivity().findViewById(R.id.profileLastName);
        dateOfBirth = (TextView) getActivity().findViewById(R.id.profileDateOfBirth);
        nationality = (TextView) getActivity().findViewById(R.id.profileNationality);
        gender = (TextView) getActivity().findViewById(R.id.profileGender);
        relationshipStatus = (TextView) getActivity().findViewById(R.id.profileRelationshipStatus);
        language = (TextView) getActivity().findViewById(R.id.profileLanguage);
        aboutMe = (TextView) getActivity().findViewById(R.id.profileAboutMe);

        // Edit fields
        editUserPicture = (ImageView) getActivity().findViewById(R.id.profileEditPicture);
        takePhoto = (Button) getActivity().findViewById(R.id.profileTakePicture);
        pickPhoto = (Button) getActivity().findViewById(R.id.profilePickPicture);
        editFirstName = (AhoyEditText) getActivity().findViewById(R.id.profileEditFirstName);
        editLastName = (AhoyEditText) getActivity().findViewById(R.id.profileEditLastName);
        editDateOfBirth = (AhoyButton) getActivity().findViewById(R.id.profileEditDateOfBirth);
        editNationality = (AhoySpinner) getActivity().findViewById(R.id.profileEditNationality);
        editGender = (AhoySpinner) getActivity().findViewById(R.id.profileEditGender);
        editRelationshipStatus = (AhoySpinner) getActivity().findViewById(R.id.profileEditRelationshipStatus);
        editLanguage = (AhoyEditText) getActivity().findViewById(R.id.profileEditLanguage);
        editAboutMe = (AhoyEditText) getActivity().findViewById(R.id.profileEditAboutMe);

        // Buttons
        editBasicInformation = (Button) getActivity().findViewById(R.id.profileEditBasicInformation);
        saveBasicInformation = (Button) getActivity().findViewById(R.id.profileSaveBasicInformation);
        cancelBasicInformation = (Button) getActivity().findViewById(R.id.profileCancelBasicInformation);

        // Switcher to switch between edit and display
        switcherBasicInformation = (ViewSwitcher) getActivity().findViewById(R.id.profileBasicInformation);
    }

    private void setListeners(final Profile profile) {
        // Inputfields and showing fields for basic
        final ProfileUpdateEditContainer basicUpdateEdits = getBasicUpdateEdits();
        final ProfileUpdateViewContainer basicUpdateViews = getBasicUpdateViews(basicUpdateEdits);

        takePhoto.setOnClickListener(new Profile_CameraButtonClickListener(getActivity()));
        pickPhoto.setOnClickListener(new Profile_PickerButtonClickListener(getActivity()));
        editBasicInformation.setOnClickListener(new Profile_EditListener(switcherBasicInformation, userPicture, editUserPicture));
        cancelBasicInformation.setOnClickListener(new Profile_CancelListener(switcherBasicInformation));

        saveBasicInformation.setOnClickListener(new Profile_SaveListener(getActivity(), switcherBasicInformation, ServerPaths.PROFILEBASIC, basicUpdateEdits, basicUpdateViews,
                userPicture, editUserPicture));

        // DatePicker
        if (profile.getDateOfBirth() == null) {
            editDateOfBirth.setOnClickListener(new Ahoy_DateButtonListener(getActivity(), editDateOfBirth));
        } else {
            editDateOfBirth.setOnClickListener(new Ahoy_DateButtonListener(getActivity(), editDateOfBirth, profile.getDateOfBirth()));
        }
    }

    private ProfileUpdateViewContainer getBasicUpdateViews(final ProfileUpdateEditContainer basicUpdateEdits) {
        final ProfileUpdateViewContainer basicUpdateViews = new ProfileUpdateViewContainerImpl(basicUpdateEdits, getActivity());
        basicUpdateViews.add(ProfileField.VIEWFIRSTNAME, firstName);
        basicUpdateViews.add(ProfileField.VIEWLASTNAME, lastName);
        basicUpdateViews.add(ProfileField.VIEWDATEOFBIRTH, dateOfBirth);
        basicUpdateViews.add(ProfileField.VIEWNATIONALITY, nationality);
        basicUpdateViews.add(ProfileField.VIEWGENDER, gender);
        basicUpdateViews.add(ProfileField.VIEWRELATIONSHIPSTATUS, relationshipStatus);
        basicUpdateViews.add(ProfileField.VIEWLANGUAGE, language);
        basicUpdateViews.add(ProfileField.VIEWABOUTME, aboutMe);
        return basicUpdateViews;
    }

    private ProfileUpdateEditContainer getBasicUpdateEdits() {
        final ProfileUpdateEditContainer basicUpdateEdits = new ProfileUpdateEditContainerImpl();
        basicUpdateEdits.add(ProfileField.EDITFIRSTNAME, editFirstName);
        basicUpdateEdits.add(ProfileField.EDITLASTNAME, editLastName);
        basicUpdateEdits.add(ProfileField.EDITDATEOFBIRTH, editDateOfBirth);
        basicUpdateEdits.add(ProfileField.EDITNATIONALITY, editNationality);
        basicUpdateEdits.add(ProfileField.EDITGENDER, editGender);
        basicUpdateEdits.add(ProfileField.EDITRELATIONSHIPSTATUS, editRelationshipStatus);
        basicUpdateEdits.add(ProfileField.EDITLANGUAGE, editLanguage);
        basicUpdateEdits.add(ProfileField.EDITABOUTME, editAboutMe);
        return basicUpdateEdits;
    }

    private void setEditAdapters(final Profile profile) {
        // fill Spinners
        adapterNationality = new ArrayAdapter<Countries>(getActivity(), android.R.layout.simple_spinner_item, Countries.values());
        adapterNationality.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editNationality.setAdapter(adapterNationality);

        adapterGender = new ArrayAdapter<Gender>(getActivity(), android.R.layout.simple_spinner_item, Gender.values());
        adapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editGender.setAdapter(adapterGender);

        adapterRelationshipStatus = new ArrayAdapter<RelationshipStatus>(getActivity(), android.R.layout.simple_spinner_item, RelationshipStatus.values());
        adapterRelationshipStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editRelationshipStatus.setAdapter(adapterRelationshipStatus);

        updateBasicInformationEditFieldsFromProfile(profile);
    }

    private void updateBasicInformationEditFieldsFromProfile(final Profile profile) {
        editUserPicture.setImageDrawable(userPicture.getDrawable());
        editFirstName.setText(profile.getFirstName());
        editLastName.setText(profile.getLastName());
        editDateOfBirth.setText(profile.dateOfBirthAsString());
        editNationality.setSelection(adapterNationality.getPosition(profile.getNationality()));
        editGender.setSelection(adapterGender.getPosition(profile.getGender()));
        editRelationshipStatus.setSelection(adapterRelationshipStatus.getPosition(profile.getRelationshipStatus()));
        editLanguage.setText(profile.getLanguage());
        editAboutMe.setText(profile.getAboutMe());
    }

    private void updateBasicInformationViewFieldsFromProfile(final Profile profile) {
        new DownloadProfilePictureTask(getActivity(), userPicture, editUserPicture).execute(profile.getImageResource());
        firstName.setText(profile.getFirstName());
        lastName.setText(profile.getLastName());
        dateOfBirth.setText(profile.dateOfBirthAsString());
        nationality.setText(profile.getNationality().getValue());
        gender.setText(profile.getGender().getValue());
        relationshipStatus.setText(profile.getRelationshipStatus().getValue());
        language.setText(profile.getLanguage());
        aboutMe.setText(profile.getAboutMe());
    }
}

