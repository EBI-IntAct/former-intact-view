package uk.ac.ebi.intact.psicquic.ws;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.arp.JenaReader;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.model.BioPAXLevel;
import org.hupo.psi.mi.psicquic.*;
import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.xml.PsimiXmlReaderException;
import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.PsimiXmlWriter;
import psidev.psi.mi.xml.PsimiXmlWriterException;
import psidev.psi.mi.xml.converter.ConverterException;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml254.jaxb.EntrySet;
import uk.ac.ebi.intact.psicquic.ws.config.PsicquicConfig;
import uk.ac.ebi.intact.psicquic.ws.util.PsicquicStreamingOutput;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This web service is based on a PSIMITAB SOLR index to search and return the results.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: IntactPsicquicService.java 12873 2009-03-18 02:51:31Z baranda $
 */
@Controller
public class IntactPsicquicRestService implements PsicquicRestService {

     @Autowired
    private PsicquicConfig config;

    @Autowired
    private PsicquicService psicquicService;

    public Object getByInteractor(String interactorAc, String db, String format, String firstResult, String maxResults) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = "id:"+createQueryValue(interactorAc, db)+ " OR alias:"+createQueryValue(interactorAc, db);
        return getByQuery(query, format, firstResult, maxResults);
    }

    public Object getByInteraction(String interactionAc, String db, String format, String firstResult, String maxResults) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = "interaction_id:"+createQueryValue(interactionAc, db);
        return getByQuery(query, format, firstResult, maxResults);
    }

    public Object getByQuery(String query, String format,
                                                 String firstResultStr,
                                                 String maxResultsStr) throws PsicquicServiceException,
                                                                 NotSupportedMethodException,
                                                                 NotSupportedTypeException {
        // apply any filter
        if (config.getQueryFilter() != null && !config.getQueryFilter().isEmpty()) {
            if ("*".equals(query) || query.trim().isEmpty()) {
                query = config.getQueryFilter();
            } else {
                query = query + " "+config.getQueryFilter();
                query = query.trim();
            }
        }


        int firstResult;
        int maxResults;

        try {
            firstResult =Integer.parseInt(firstResultStr);
        } catch (NumberFormatException e) {
            throw new PsicquicServiceException("firstResult parameter is not a number: "+firstResultStr);
        }

        try {
            if (maxResultsStr == null) {
                maxResults = Integer.MAX_VALUE;
            } else {
                maxResults = Integer.parseInt(maxResultsStr);
            }
        } catch (NumberFormatException e) {
            throw new PsicquicServiceException("maxResults parameter is not a number: "+maxResultsStr);
        }

        format = format.toLowerCase();

        try {
            if (strippedMime(IntactPsicquicService.RETURN_TYPE_XML25).equalsIgnoreCase(format)) {
                final EntrySet entrySet = getByQueryXml(query, firstResult, maxResults);
                return Response.status(200).type(MediaType.APPLICATION_XML_TYPE).entity(entrySet).build();
            } else if (format.toLowerCase().startsWith("rdf")) {
                String rdfFormat = getRdfFormatName(format);
                String mediaType = format.contains("xml")? MediaType.APPLICATION_XML : MediaType.TEXT_PLAIN;

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                convertToBioPAX(os, BioPAXLevel.L3, query, firstResult, maxResults);

                final String baseUri = "http://www.ebi.ac.uk/intact/";

                OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

                final RDFReader rdfReader = new JenaReader();
                rdfReader.read(model, new StringReader(os.toString()), baseUri);

                final RDFWriter rdfWriter = model.getWriter(rdfFormat);
                rdfWriter.setProperty("xmlbase", baseUri);
                model.setNsPrefix("", baseUri);

                Writer writer = new StringWriter();
                rdfWriter.write(model, writer, baseUri);
                
                String rdfOutputStr = writer.toString();
                
                return Response.status(200).type(mediaType).entity(rdfOutputStr).build();

            } else if (format.startsWith(IntactPsicquicService.RETURN_TYPE_BIOPAX)) {
                String[] formatElements = format.split("-");
                String biopaxLevel = "L3";

                if (formatElements.length == 2) {
                    biopaxLevel = formatElements[1].toUpperCase();
                    biopaxLevel = biopaxLevel.replaceAll("LEVEL", "L");
                }

                BioPAXLevel bioPAXLevel = BioPAXLevel.valueOf(biopaxLevel);

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                convertToBioPAX(os, bioPAXLevel, query, firstResult, maxResults);

                return Response.status(200).type(MediaType.APPLICATION_XML_TYPE).entity(os.toString()).build();

            } else if (IntactPsicquicService.RETURN_TYPE_COUNT.equalsIgnoreCase(format)) {
                return count(query);
            } else if (strippedMime(IntactPsicquicService.RETURN_TYPE_MITAB25_BIN).equalsIgnoreCase(format)) {
                PsicquicStreamingOutput result = new PsicquicStreamingOutput(psicquicService, query, firstResult, maxResults, true);
                return Response.status(200).type("application/x-gzip").entity(result).build();
            } else if (strippedMime(IntactPsicquicService.RETURN_TYPE_MITAB25).equalsIgnoreCase(format) || format == null) {
                PsicquicStreamingOutput result = new PsicquicStreamingOutput(psicquicService, query, firstResult, maxResults);
                return Response.status(200).type(MediaType.TEXT_PLAIN).entity(result).build();
            } else {
                return Response.status(406).type(MediaType.TEXT_PLAIN).entity("Format not supported").build();
            }
        } catch (Throwable e) {
            throw new PsicquicServiceException("Problem creating output", e);
        }

    }

    protected void convertToBioPAX(OutputStream os, BioPAXLevel biopaxLevel, String query, int firstResult, int maxResults) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException, ConverterException, PsimiXmlWriterException, IOException, PsimiXmlReaderException {
        final EntrySet entrySet254 = getByQueryXml(query, firstResult, maxResults);

        EntrySetConverter converter254 = new EntrySetConverter();
        converter254.setDAOFactory(new InMemoryDAOFactory());

        final psidev.psi.mi.xml.model.EntrySet entrySet = converter254.fromJaxb(entrySet254);

        ByteArrayOutputStream psimiData = new ByteArrayOutputStream();

        PsimiXmlWriter psiWriter = new PsimiXmlWriter(PsimiXmlVersion.VERSION_254);
        psiWriter.write(entrySet, psimiData);

        InputStream is = new ByteArrayInputStream(psimiData.toByteArray());
        PSIMIBioPAXConverter biopaxConverter = new PSIMIBioPAXConverter(biopaxLevel);

        biopaxConverter.convert(is, os);
    }

    private String getRdfFormatName(String format) {
        format = format.substring(4);

        String rdfFormat;

        if ("xml".equalsIgnoreCase(format)) {
            rdfFormat = "RDF/XML";
        } else if ("xml-abbrev".equalsIgnoreCase(format)) {
            rdfFormat = "RDF/XML-ABBREV";
        } else {
            rdfFormat = format.toUpperCase();
        }

        return rdfFormat;
    }

    protected psidev.psi.mi.xml.model.EntrySet createEntrySet(String query, int firstResult, int maxResults) throws ConverterException, PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        EntrySetConverter converter = new EntrySetConverter();
        converter.setDAOFactory(new InMemoryDAOFactory());
        psidev.psi.mi.xml.model.EntrySet entrySet = converter.fromJaxb(getByQueryXml(query, firstResult, maxResults));
        return entrySet;
    }

    public Object getSupportedFormats() throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        List<String> formats = new ArrayList<String>(IntactPsicquicService.SUPPORTED_RETURN_TYPES.size()+1);
        formats.add(strippedMime(IntactPsicquicService.RETURN_TYPE_MITAB25_BIN));

        for (String mime : IntactPsicquicService.SUPPORTED_RETURN_TYPES) {
            formats.add(strippedMime(mime));
        }

        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(StringUtils.join(formats, "\n")).build();
    }

    public Object getProperty(String propertyName) {
        final String val = config.getProperties().get(propertyName);

        if (val == null) {
            return Response.status(404)
                .type(MediaType.TEXT_PLAIN)
                .entity("Property not found: "+propertyName).build();
        }

         return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(val).build();
    }

    public Object getProperties() {
        StringBuilder sb = new StringBuilder(256);

        for (Map.Entry entry : config.getProperties().entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return Response.status(200)
                .type(MediaType.TEXT_PLAIN)
                .entity(sb.toString()).build();
    }

    public String getVersion() {
        return config.getVersion();
    }

    public EntrySet getByQueryXml(String query,
                                  int firstResult,
                                  int maxResults) throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        RequestInfo reqInfo = new RequestInfo();
        reqInfo.setResultType("psi-mi/xml25");

        try {
            reqInfo.setFirstResult(firstResult);
        } catch (NumberFormatException e) {
            throw new PsicquicServiceException("firstResult parameter is not a number: "+firstResult);
        }

        try {
            reqInfo.setBlockSize(maxResults);
        } catch (NumberFormatException e) {
            throw new PsicquicServiceException("maxResults parameter is not a number: "+maxResults);
        }

        QueryResponse response = psicquicService.getByQuery(query, reqInfo);

        return response.getResultSet().getEntrySet();
    }

    private int count(String query) throws NotSupportedTypeException, NotSupportedMethodException, PsicquicServiceException {
        RequestInfo reqInfo = new RequestInfo();
        reqInfo.setResultType("count");
        QueryResponse response = psicquicService.getByQuery(query, reqInfo);
        return response.getResultInfo().getTotalResults();
    }

    private String createQueryValue(String interactorAc, String db) {
        StringBuilder sb = new StringBuilder(256);
        if (db.length() > 0) sb.append('"').append(db).append(':');
        sb.append(interactorAc);
        if (db.length() > 0) sb.append('"');

        return sb.toString();
    }

    private String strippedMime(String mimeType) {
        if (mimeType.indexOf("/") > -1) {
            return mimeType.substring(mimeType.indexOf("/")+1);
        } else {
            return mimeType;
        }
    }
}
