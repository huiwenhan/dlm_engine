//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-833 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.02.27 at 05:15:54 PM GMT+05:30 
//


package org.apache.ivory.oozie.workflow;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for JAVA complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="JAVA">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="job-tracker" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="name-node" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="prepare" type="{uri:oozie:workflow:0.3}PREPARE" minOccurs="0"/>
 *         &lt;element name="job-xml" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="configuration" type="{uri:oozie:workflow:0.3}CONFIGURATION" minOccurs="0"/>
 *         &lt;element name="main-class" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="java-opts" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="arg" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="archive" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="capture-output" type="{uri:oozie:workflow:0.3}FLAG" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JAVA", propOrder = {
    "jobTracker",
    "nameNode",
    "prepare",
    "jobXml",
    "configuration",
    "mainClass",
    "javaOpts",
    "arg",
    "file",
    "archive",
    "captureOutput"
})
public class JAVA {

    @XmlElement(name = "job-tracker", required = true)
    protected String jobTracker;
    @XmlElement(name = "name-node", required = true)
    protected String nameNode;
    protected PREPARE prepare;
    @XmlElement(name = "job-xml")
    protected String jobXml;
    protected CONFIGURATION configuration;
    @XmlElement(name = "main-class", required = true)
    protected String mainClass;
    @XmlElement(name = "java-opts")
    protected String javaOpts;
    protected List<String> arg;
    protected List<String> file;
    protected List<String> archive;
    @XmlElement(name = "capture-output")
    protected FLAG captureOutput;

    /**
     * Gets the value of the jobTracker property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJobTracker() {
        return jobTracker;
    }

    /**
     * Sets the value of the jobTracker property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJobTracker(String value) {
        this.jobTracker = value;
    }

    /**
     * Gets the value of the nameNode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNameNode() {
        return nameNode;
    }

    /**
     * Sets the value of the nameNode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNameNode(String value) {
        this.nameNode = value;
    }

    /**
     * Gets the value of the prepare property.
     * 
     * @return
     *     possible object is
     *     {@link PREPARE }
     *     
     */
    public PREPARE getPrepare() {
        return prepare;
    }

    /**
     * Sets the value of the prepare property.
     * 
     * @param value
     *     allowed object is
     *     {@link PREPARE }
     *     
     */
    public void setPrepare(PREPARE value) {
        this.prepare = value;
    }

    /**
     * Gets the value of the jobXml property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJobXml() {
        return jobXml;
    }

    /**
     * Sets the value of the jobXml property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJobXml(String value) {
        this.jobXml = value;
    }

    /**
     * Gets the value of the configuration property.
     * 
     * @return
     *     possible object is
     *     {@link CONFIGURATION }
     *     
     */
    public CONFIGURATION getConfiguration() {
        return configuration;
    }

    /**
     * Sets the value of the configuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link CONFIGURATION }
     *     
     */
    public void setConfiguration(CONFIGURATION value) {
        this.configuration = value;
    }

    /**
     * Gets the value of the mainClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Sets the value of the mainClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMainClass(String value) {
        this.mainClass = value;
    }

    /**
     * Gets the value of the javaOpts property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJavaOpts() {
        return javaOpts;
    }

    /**
     * Sets the value of the javaOpts property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJavaOpts(String value) {
        this.javaOpts = value;
    }

    /**
     * Gets the value of the arg property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the arg property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getArg().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getArg() {
        if (arg == null) {
            arg = new ArrayList<String>();
        }
        return this.arg;
    }

    /**
     * Gets the value of the file property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the file property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getFile() {
        if (file == null) {
            file = new ArrayList<String>();
        }
        return this.file;
    }

    /**
     * Gets the value of the archive property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the archive property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getArchive().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getArchive() {
        if (archive == null) {
            archive = new ArrayList<String>();
        }
        return this.archive;
    }

    /**
     * Gets the value of the captureOutput property.
     * 
     * @return
     *     possible object is
     *     {@link FLAG }
     *     
     */
    public FLAG getCaptureOutput() {
        return captureOutput;
    }

    /**
     * Sets the value of the captureOutput property.
     * 
     * @param value
     *     allowed object is
     *     {@link FLAG }
     *     
     */
    public void setCaptureOutput(FLAG value) {
        this.captureOutput = value;
    }

}
