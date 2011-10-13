/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import uk.gov.nationalarchives.droid.core.interfaces.filter.Filter;
import uk.gov.nationalarchives.droid.core.interfaces.filter.FilterCriterion;
import uk.gov.nationalarchives.droid.core.interfaces.filter.FilterValue;

/**
 * @author Alok Kumar Dash
 * 
 */

@XmlAccessorType(XmlAccessType.NONE)
public class FilterImpl implements Cloneable, Filter {

    @XmlElement(name = "Enabled", required = true)
    private boolean enabled;

    @XmlElement(name = "Narrowed", required = true)
    private boolean narrowed;

    private Map<Integer, FilterCriterionImpl> filterCriteriaMap = new TreeMap<Integer, FilterCriterionImpl>();

    // this only to save in xml --- begins

    /**
     * Getter method for filter criteria list.
     * 
     * @return List of filter criteria.
     */
    public List<FilterCriterion> getCriteria() {
        return new ArrayList<FilterCriterion>(filterCriteriaMap.values());
    }

    /**
     * Getter for XML filter criteria.
     * 
     * @return the filter criteria
     */
    @XmlElement(name = "Criteria", required = true)
    List<FilterCriterionImpl> getFilterCriteria() {
        return new ArrayList<FilterCriterionImpl>(filterCriteriaMap.values());
    }

    /**
     * Setter for XML filter criteria.
     * 
     * @param criteria
     *            the criteria to set
     */
    void setFilterCriteria(List<FilterCriterionImpl> criteria) {
        int index = 0;
        for (FilterCriterionImpl criterion : criteria) {
            filterCriteriaMap.put(index++, criterion);
        }
    }

    /**
     * Setter method for criteria list.
     * 
     * @param criteria
     *            criteria list to set.
     */
    public void setCriteria(List<FilterCriterionImpl> criteria) {
        int index = 0;
        for (FilterCriterionImpl criterion : criteria) {
            filterCriteriaMap.put(index++, criterion);
        }
    }

    // this only to save in xml -- ends

    /**
     * Add filter criteria to list.
     * 
     * @param filterCriteria
     *            filter criteria to add.
     * @param index
     *            position of filter criteria in selected filter dialog.
     */
    public void addFilterCiterion(FilterCriterionImpl filterCriteria, int index) {
        if (getFilterCriterion(index) == null) {
            filterCriteriaMap.put(index, filterCriteria);
        } else {
            filterCriteriaMap.remove(index);
            filterCriteriaMap.put(index, filterCriteria);
        }
    }

    /**
     * remove filter criteria from the list.
     * 
     * @param rowNumber
     *            position of filter criteria in selected filter dialog.
     */
    public void removeFilterCriterion(int rowNumber) {
        filterCriteriaMap.remove(rowNumber);
    }

    /**
     * Getter method for filter criteria.
     * 
     * @return Filter criteria map.
     */
    public Map<Integer, FilterCriterionImpl> getFilterCriteriaMap() {
        return filterCriteriaMap;
    }

    /**
     * 
     * @param filterCriteriaMap
     */
    private void setFilterCriteriaMap(Map<Integer, FilterCriterionImpl> filterCriteriaMap) {
        this.filterCriteriaMap = filterCriteriaMap;
    }

    /**
     * 
     * @param rowNumber
     *            position of this filter criteria at filter dialog.
     * @return FilterCriteria filter criteria for the given position.
     */
    public FilterCriterionImpl getFilterCriterion(int rowNumber) {
        return filterCriteriaMap.get(rowNumber);
    }

    /**
     * @return Object Filter. Deep copy.
     */
    @Override
    public Filter clone() {
        Map<Integer, FilterCriterionImpl> clonedFilterCriteriaMap = new HashMap<Integer, FilterCriterionImpl>();
        try {
            super.clone();
            FilterImpl clone = new FilterImpl();
            for (int i = 0; i < filterCriteriaMap.size(); i++) {
                FilterCriterionImpl cloneFilterCriteria = new FilterCriterionImpl();

                FilterCriterionImpl filterCriteria = filterCriteriaMap.get(i);

                cloneFilterCriteria.setField(filterCriteria.getField());

                cloneFilterCriteria.setOperator(filterCriteria.getOperator());
                cloneFilterCriteria.setValueFreeText(new String(filterCriteria.getValueFreeText()));

                cloneFilterCriteria.setRowNumber(new Integer(filterCriteria.getRowNumber()));

                List<FilterValue> selectedValues = filterCriteria.getSelectedValues();

                if (selectedValues != null) {
                    List<FilterValue> clonedSelectedValues = new ArrayList<FilterValue>();
                    for (int j = 0; j < selectedValues.size(); j++) {
                        FilterValue values = selectedValues.get(j);
                        FilterValue clonedValues = new FilterValue(values.getId(), values.getDescription(), values
                                .getQueryParameter());
                        clonedSelectedValues.add(clonedValues);
                    }
                    cloneFilterCriteria.setSelectedValues(clonedSelectedValues);
                }

                clonedFilterCriteriaMap.put(i, cloneFilterCriteria);
            }
            clone.setFilterCriteriaMap(clonedFilterCriteriaMap);
            clone.setEnabled(new Boolean(enabled));
            clone.setNarrowed(new Boolean(narrowed));
            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * getter method for filter enabled.
     * 
     * @return boolean true if enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Setter method for filter enabled.
     * 
     * @param enabled
     *            enabled filag.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Getter method for Any or All flag.
     * @return boolean Any or All flag in filter dialog.
     */
    public boolean isNarrowed() {
        return narrowed;
    }

    /**
     * Setter method for Any or All flag.
     * @param narrowed
     *            any or All flag in filter dialog.
     */
    public void setNarrowed(boolean narrowed) {
        this.narrowed = narrowed;
    }

    /**
     * Checks if filter is empty or filter criteria list exists.
     * @return boolean
     */
    public boolean hasCriteria() {
        boolean hasCriteria = false;
        if (filterCriteriaMap != null && !filterCriteriaMap.isEmpty()) {
            hasCriteria =  true;
        }
        return hasCriteria;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfFilterCriterion() {
        return filterCriteriaMap.size();
    }


}
