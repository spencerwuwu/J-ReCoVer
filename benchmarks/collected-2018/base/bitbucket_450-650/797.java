// https://searchcode.com/api/result/40278032/

package org.openxdata.openmrs.db;

import java.util.Vector;

import org.openxdata.db.util.Storage;
import org.openxdata.db.util.StorageFactory;
import org.openxdata.db.util.StorageListener;
import org.openxdata.model.OpenXdataConstants;
import org.openxdata.model.StudyDef;
import org.openxdata.openmrs.CohortList;
import org.openxdata.openmrs.MedicalHistoryList;
import org.openxdata.openmrs.Patient;
import org.openxdata.openmrs.PatientData;
import org.openxdata.openmrs.PatientField;
import org.openxdata.openmrs.PatientFieldList;
import org.openxdata.openmrs.PatientFieldValue;
import org.openxdata.openmrs.PatientFieldValueList;
import org.openxdata.openmrs.PatientForm;
import org.openxdata.openmrs.PatientList;
import org.openxdata.openmrs.PatientMedicalHistory;


/**
 * Handles data storage operations for openmrs.
 * 
 * @author Daniel Kayiwa
 *
 */
public class OpenmrsDataStorage {

	/** The unique identifier for storage of patients. */
	private static final String PATIENT_STORAGE_NAME = "Patient";

	/** The unique identifier for storage of patient fields. */
	private static final String PATIENT_FIELD_STORAGE_NAME = "PatientFieldList";

	/** The unique identifier for storage of cohorts. */
	private static final String COHORT_STORAGE_NAME = "CohortList";

	/** The unique identifier for storage of patient field values. */
	private static final String PATIENT_FIELD_VALUE_STORAGE_NAME = "PatientFieldValueList";

	/** The unique identifier for storage of patient form record mappings. */
	private static final String PATIENT_FORM_STORAGE_NAME = "PatientForm";

	/** The unique identifier for storage of patient medical history. */
	private static final String PATIENT_HISTORY_STORAGE_NAME = "PatientMedicalHistory";

	public static StorageListener storageListener;

	/**
	 * Saves a patient form mapping.
	 * 
	 * @param formDefId - the form definition identifier.
	 * @param patientForm - the patient form mapping data
	 */
	public static void savePatientForm(int formDefId, PatientForm patientForm){
		StorageFactory.getStorage(getPatientFormStorageName(formDefId),storageListener).save(patientForm); //These records are not edited, they are only saved new and deleted.
	}

	/**
	 * Saves a patient.
	 * 
	 * @param patient - the patient to be saved.
	 */
	public static void savePatient(Patient patient){
		StorageFactory.getStorage(PATIENT_STORAGE_NAME,storageListener).save(patient);
	}

	/**
	 * Saves patients.
	 * 
	 * @param patients - the list of patients to be saved.
	 */
	public static void savePatients(PatientList patients){
		Storage store = StorageFactory.getStorage(PATIENT_STORAGE_NAME,storageListener);
		store.delete();
		for(int i=0; i<patients.size(); i++)
			store.save(patients.getPatient(i));
	}

	/**
	 * Saves patient data. All existing data is first deleted before
	 * the new one is saved.
	 * 
	 * @param patientData - the patient data to be saved.
	 */
	public static void savePatientData(PatientData patientData, boolean append){
		if(patientData == null)
			return;

		Storage store = null;

		PatientList patients = patientData.getPatients();
		if(patients != null && patients.size() > 0){
			store = StorageFactory.getStorage(PATIENT_STORAGE_NAME,storageListener);
			if(!append)
				store.delete();
			for(int i=0; i<patients.size(); i++)
				store.save(patients.getPatient(i));
		}
		
		PatientFieldList fields = patientData.getFields();
		if(fields != null && fields.size() > 0){
			store = StorageFactory.getStorage(PATIENT_FIELD_STORAGE_NAME,storageListener);
			if(!append){
				store.delete();
				store.addNew(fields);
			}
			else{
				PatientFieldList oldFields = getPatientFields();
				if(oldFields == null)
					store.addNew(fields);
				else{
					updatePatientFields(oldFields,fields);
					store.delete();
					store.addNew(oldFields);
				}
			}
		}

		PatientFieldValueList values = patientData.getFieldValues();
		if(values != null && values.size() > 0){
			store = StorageFactory.getStorage(PATIENT_FIELD_VALUE_STORAGE_NAME,storageListener);
			if(!append){
				store.delete();
				store.addNew(values);
			}
			else{
				PatientFieldValueList oldValues = getPatientFieldValues();
				if(oldValues == null)
					store.addNew(values);
				else{
					updatePatientFieldValues(oldValues,values);
					store.delete();
					store.addNew(oldValues);
				}
			}
		}
		
		MedicalHistoryList history = patientData.getHistory();
		
		if(history != null && history.size() > 0){
			store = StorageFactory.getStorage(PATIENT_HISTORY_STORAGE_NAME,storageListener);
			if(!append){
				store.delete();
				store.addNew(history);
			}
			else{
				MedicalHistoryList oldHistory = getMedicalHistory();
				if(oldHistory == null)
					store.addNew(history);
				else{
					updateMedicalHistory(oldHistory,history);
					store.delete();
					store.addNew(oldHistory);
				}
			}
		}		
	}

	private static PatientMedicalHistory getPatientMedicalHistory(int patientId, MedicalHistoryList history){
		PatientMedicalHistory patientHistory;
		for(int index = 0; index < history.size(); index++){
			patientHistory = history.getHistory(index);
			if(patientHistory.getPatientId() == patientId)
				return patientHistory;
		}
		return null;
	}

	private static void updateMedicalHistory(MedicalHistoryList oldHistory, MedicalHistoryList newHistory){
		PatientMedicalHistory newPatientHistory, oldPatientHistory;
		for(int index = 0; index < newHistory.size(); index++){
			newPatientHistory = newHistory.getHistory(index);
			oldPatientHistory = getPatientMedicalHistory(newPatientHistory.getPatientId(),oldHistory);
			if(oldPatientHistory == null)
				oldHistory.addHistory(newPatientHistory);
			else
				oldPatientHistory.setHistory(newPatientHistory.getHistory());
		}
	}

	private static PatientFieldValue getPatientFieldValue(int patientId, int fieldId, PatientFieldValueList values){
		PatientFieldValue value;
		for(int index = 0; index < values.size(); index++){
			value = values.getValue(index);
			if(value.getPatientId() == patientId && value.getFieldId() == fieldId)
				return value;
		}
		return null;
	}

	private static void updatePatientFieldValues(PatientFieldValueList oldFieldValues, PatientFieldValueList newFieldValues){
		PatientFieldValue newPatientFieldValue, oldPatientFieldValue;
		for(int index = 0; index < newFieldValues.size(); index++){
			newPatientFieldValue = newFieldValues.getValue(index);
			oldPatientFieldValue = getPatientFieldValue(newPatientFieldValue.getPatientId(),newPatientFieldValue.getFieldId(),oldFieldValues);
			if(oldPatientFieldValue == null)
				oldFieldValues.addValue(newPatientFieldValue);
			else
				oldPatientFieldValue.setValue(newPatientFieldValue.getValue());
		}
	}

	private static PatientField getPatientField(int fieldId, PatientFieldList fields){
		PatientField field;
		for(int index = 0; index < fields.size(); index++){
			field = fields.getField(index);
			if(field.getId() == fieldId)
				return field;
		}
		return null;
	}

	private static void updatePatientFields(PatientFieldList oldFields, PatientFieldList newFields){
		PatientField newPatientField, oldPatientField;
		for(int index = 0; index < newFields.size(); index++){
			newPatientField = newFields.getField(index);
			oldPatientField = getPatientField(newPatientField.getId(),oldFields);
			if(oldPatientField == null)
				oldFields.addField(newPatientField);
			else
				oldPatientField.setName(newPatientField.getName()); //TODO This is a bug if the number of fields at the server changes
		}
	}

	public static void deleteNewPatients(){
		Vector patients = getPatients();
		if(patients == null)
			return;

		Storage	store = StorageFactory.getStorage(PATIENT_STORAGE_NAME,storageListener);
		for(int i=0; i<patients.size(); i++){
			Patient patient = (Patient)patients.elementAt(i);
			if(patient.isNewPatient())
				store.delete(patient);
		}
	}

	/**
	 * Saves cohort data. All existing data is first deleted before
	 * the new one is saved.
	 * 
	 * @param cohorts - the cohort list.
	 */
	public static void saveCohorts(CohortList cohorts){
		if(cohorts == null)
			return;

		Storage store = null;
		if(cohorts != null && cohorts.size() > 0){
			store = StorageFactory.getStorage(COHORT_STORAGE_NAME,storageListener);
			store.delete();
			store.addNew(cohorts);
		}
	}

	/**
	 * Gets a list of patients.
	 * 
	 * @return - a list of patients.
	 */
	public static Vector getPatients(){
		return StorageFactory.getStorage(PATIENT_STORAGE_NAME,storageListener).read(new Patient().getClass());
	}

	/**
	 * Gets a list of patients matching a name and or a patient identifier parameters.
	 * Patients whose names and identifier contain the search parameters
	 * will be returned. In otherwards, not an exact match.
	 * If a parameter is empty or null, all patients are considered to match that parameter.
	 * For instance, if you pass null or empty name and identifier, all patients
	 * will be returned.
	 * 
	 * @param name - the name to search for
	 * @param identifier - the identifier
	 * @return
	 */
	public static Vector getPatients(String identifier,String name){
		Vector patients  = getPatients();
		Vector matchedPatients = new Vector();

		if(patients != null){
			Patient patient;
			for(int i=0; i<patients.size(); i++){
				patient = (Patient)patients.elementAt(i);
				if(doesPatientMatch(identifier,name,patient))
					matchedPatients.addElement(patient);
			}
		}

		return matchedPatients;
	}

	/**
	 * Checks whether a patient matches the search parameters.
	 * 
	 * @param name - the name parameters.
	 * @param identifier - the identifier parameter.
	 * @param patient - the patient.
	 * @return - true if the patient matches, else false.
	 */
	private static boolean doesPatientMatch( String searchIdentifier,String searchName, Patient patient){
		boolean nameMatch = false;
		boolean identifierMatch = false;

		if(searchName == null || searchName.equals(""))
			nameMatch = true;

		if(searchIdentifier == null || searchIdentifier.equals(""))
			identifierMatch = true;

		if(!identifierMatch)
			identifierMatch = doesPatternMatch(searchIdentifier,patient.getPatientIdentifier());

		if(!nameMatch)
			nameMatch = doesPatternMatch(searchName,patient.getFamilyName());

		if(!nameMatch)
			nameMatch = doesPatternMatch(searchName,patient.getMiddleName());

		if(!nameMatch)
			nameMatch = doesPatternMatch(searchName,patient.getGivenName());

		return nameMatch && identifierMatch;
	}

	private static boolean doesPatternMatch(String searchPattern, String value){
		if(value != null && value.toLowerCase().indexOf(searchPattern.toLowerCase()) != -1)
			return true;
		return false;
	}

	/**
	 * Gets patient database fields.
	 * 
	 * @return - patient database field list object.
	 */
	public static PatientFieldList getPatientFields(){
		Storage store = StorageFactory.getStorage(PATIENT_FIELD_STORAGE_NAME,storageListener);
		PatientFieldList fieldList = null;
		Vector vect = store.read(new PatientFieldList().getClass()); 
		if(vect != null && vect.size() > 0)
			fieldList = (PatientFieldList)vect.elementAt(0); //We can only have one per storage.
		return fieldList;
	}

	/**
	 * Gets cohorts.
	 * 
	 * @return - cohort list object.
	 */
	public static CohortList getCohorts(){
		Storage store = StorageFactory.getStorage(COHORT_STORAGE_NAME,storageListener);
		CohortList cohortList = null;
		Vector vect = store.read(new CohortList().getClass()); 
		if(vect != null && vect.size() > 0)
			cohortList = (CohortList)vect.elementAt(0); //We can only have one per storage.
		return cohortList;
	}

	/**
	 * Gets patient database field values.
	 * 
	 * @return - patient database field value list object.
	 */
	public static PatientFieldValueList getPatientFieldValues(){
		Storage store = StorageFactory.getStorage(PATIENT_FIELD_VALUE_STORAGE_NAME,storageListener);
		PatientFieldValueList fieldValueList = null;
		Vector vect = store.read(new PatientFieldValueList().getClass());
		if(vect != null && vect.size() > 0)
			fieldValueList = (PatientFieldValueList)vect.elementAt(0); //We can only have one per storage.
		return fieldValueList;
	}

	/**
	 * Gets patient medical history.
	 * 
	 * @return - patient medical history list object.
	 */
	public static MedicalHistoryList getMedicalHistory(){
		Storage store = StorageFactory.getStorage(PATIENT_HISTORY_STORAGE_NAME,storageListener);
		MedicalHistoryList medicalHistory = null;
		Vector vect = store.read(new MedicalHistoryList().getClass());
		if(vect != null && vect.size() > 0)
			medicalHistory = (MedicalHistoryList)vect.elementAt(0); //We can only have one per storage.
		return medicalHistory;
	}

	/**
	 * 
	 * @param patientId
	 * @return
	 */
	public static PatientMedicalHistory getMedicalHistory(int patientId){
		MedicalHistoryList medicalHistory = getMedicalHistory();

		if(medicalHistory != null){
			for(int index = 0; index < medicalHistory.size(); index++){
				PatientMedicalHistory history = medicalHistory.getHistory(index);

				if(history.getPatientId() == patientId)
					return history;
			}
		}

		return null;
	}

	/**
	 * Deletes a patient.
	 * 
	 * When you delete a patient, all forms collected about them are deleted
	 * including the form that created this patient.
	 * Is this really necessary functionality?
	 * 
	 * @param data - the patient to be deleted.
	 */
	public static void deletePatient(Patient patient){
		Storage store = StorageFactory.getStorage(PATIENT_STORAGE_NAME,storageListener);
		store.delete(patient);

		PatientFieldValueList fieldValues = getPatientFieldValues();
		for(int index = 0; index < fieldValues.size(); index++){
			PatientFieldValue value = fieldValues.getValue(index);
			if(value.getPatientId() == patient.getPatientId().intValue()){
				fieldValues.remove(value);
				store = StorageFactory.getStorage(PATIENT_FIELD_VALUE_STORAGE_NAME,storageListener);
				store.delete();
				store.addNew(fieldValues);
				break;
			}
		}

		MedicalHistoryList medicalHistory = getMedicalHistory();
		for(int index = 0; index < medicalHistory.size(); index++){
			PatientMedicalHistory history = medicalHistory.getHistory(index);
			if(history.getPatientId() == patient.getPatientId().intValue()){
				medicalHistory.remove(history);
				store = StorageFactory.getStorage(PATIENT_HISTORY_STORAGE_NAME,storageListener);
				store.delete();
				store.addNew(medicalHistory);
				break;
			}
		}
	}

	/**
	 * Delete all patients from storage.
	 *
	 */
	/*public static void deletePatients(){
		Storage store = StorageFactory.getStorage(PATIENT_STORAGE_NAME,null);
		store.delete();
	}*/

	public static PatientForm getPatientForm(Integer patientId, int formDefId){
		Storage store = StorageFactory.getStorage(getPatientFormStorageName(formDefId),storageListener);
		Vector vect = store.read(new PatientForm().getClass());
		if(vect != null && vect.size() > 0){
			PatientForm patientForm = null;
			for(int i=0; i<vect.size(); i++){
				patientForm = (PatientForm)vect.elementAt(i); 
				if(patientId.equals(patientForm.getPatientId()))
					return patientForm;
			}
		}
		return null;
	}

	public static void deletePatientForm(Integer patientId, int formDefId){
		PatientForm patientForm = getPatientForm(patientId,formDefId);
		if(patientForm != null)
			StorageFactory.getStorage(getPatientFormStorageName(formDefId),storageListener).delete(patientForm);
	}

	public static void deletePatientForms(Vector studies){
		for(int i=0; i<studies.size(); i++)
			deletePatientForms((StudyDef)studies.elementAt(i));
	}

	public static void deletePatientForms(StudyDef studyDef){
		for(byte i=0; i<studyDef.getForms().size(); i++)
			StorageFactory.getStorage(getPatientFormStorageName(studyDef.getFormAt(i).getId()),storageListener).delete();
	}

	public static void deletePatientForms(Integer patientId,Vector studies){
		for(int i=0; i<studies.size(); i++)
			deletePatientForms(patientId,(StudyDef)studies.elementAt(i));
	}

	public static void deletePatientForms(Integer patientId,StudyDef studyDef){
		for(byte i=0; i<studyDef.getForms().size(); i++)
			deletePatientForm(patientId,studyDef.getFormAt(i).getId());
	}

	/**
	 * Gets the recordId of a form entered for a patient.
	 * @param patientId
	 * @param formDefId
	 * @return
	 */
	public static int getPatientFormRecordId(Integer patientId, int formDefId){
		PatientForm patientForm = getPatientForm(patientId,formDefId);
		if(patientForm != null)
			return patientForm.getFormRecordId();
		return OpenXdataConstants.NULL_ID;
	}

	public static Vector getPatientForms(int formDefId){
		return StorageFactory.getStorage(getPatientFormStorageName(formDefId),storageListener).read(new PatientForm().getClass());
	}

	/**
	 * Gets the name of the storage for patient form record mappings.
	 * For performance, patient form record mappings are stored separately
	 * for each form type. This will not only reduce the size per record
	 * but will also incread the search speed as less records will have
	 * to be searched through when locating a patient form record mapping.
	 * As a result of this, each patient will have one record in this storage.
	 * 
	 * @param - formDefId - the form definition identifier.
	 * @return - the storage name
	 */
	private static  String getPatientFormStorageName(int formDefId){
		return PATIENT_FORM_STORAGE_NAME + "." + formDefId;
	}
}

