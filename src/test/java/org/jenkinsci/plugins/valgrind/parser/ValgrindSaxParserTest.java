package org.jenkinsci.plugins.valgrind.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jenkinsci.plugins.valgrind.model.ValgrindAuxiliary;
import org.jenkinsci.plugins.valgrind.model.ValgrindError;
import org.jenkinsci.plugins.valgrind.model.ValgrindErrorKind;
import org.jenkinsci.plugins.valgrind.model.ValgrindProcess;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;
import org.jenkinsci.plugins.valgrind.model.ValgrindStacktrace;
import org.jenkinsci.plugins.valgrind.model.ValgrindStacktraceFrame;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ValgrindSaxParserTest
{
	private ValgrindSaxParser parser;
	
	@Before
	public void setup()
	{
		parser = new ValgrindSaxParser();
	}
	
	@Test
	public void simple() throws ParserConfigurationException, SAXException, IOException
	{	
		ValgrindReport report = parser.parse(new File("src/test/resources/org/jenkinsci/plugins/valgrind/parser/simple.xml"));
		
			
		assertNotNull( report );
		assertNotNull( report.getProcesses() );
		assertEquals( 1, report.getProcesses().size() );
		
		ValgrindProcess process = report.getProcesses().get(0);
		assertEquals( "10421" , process.getPid() );
		assertEquals( "./program1", process.getExecutable() );
		
		assertNotNull(process.getArguments());
		assertEquals(2, process.getArguments().size());
		assertEquals("arg1_part1 arg1_part2", process.getArguments().get(0));
		assertEquals("arg2", process.getArguments().get(1));
		
		assertEquals( 3, report.getErrorList().getErrorCount() );
		assertEquals( 3, process.getErrorList().getErrorCount() );
		
		assertEquals( 1, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.UninitCondition));
		assertEquals( 1, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.UninitValue));
		assertEquals( 1, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.Leak_DefinitelyLost));
		assertEquals( 0, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.Leak_PossiblyLost));
		
		List<ValgrindError> leakPossiblyLostErrors = report.getErrorList().getErrorsByKind(ValgrindErrorKind.Leak_PossiblyLost);
		assertNull(leakPossiblyLostErrors);
		
		List<ValgrindError> uninitConditionErrors = report.getErrorList().getErrorsByKind(ValgrindErrorKind.UninitCondition);
		assertNotNull(uninitConditionErrors);
		assertEquals( 1, uninitConditionErrors.size() );
		
		ValgrindError error = uninitConditionErrors.get(0);
		assertNotNull(error);
		
		assertEquals( ValgrindErrorKind.UninitCondition, error.getKind() );
		assertEquals( "0x2" , error.getUniqueId() );		
		assertEquals( "Conditional jump or move depends on uninitialised value(s)", error.getDescription() );
		
		assertNull( error.getLeakedBlocks() );
		assertNull( error.getLeakedBytes() );
		
		ValgrindStacktrace stacktrace = error.getStacktrace();
		assertNotNull(stacktrace);
		
		List<ValgrindStacktraceFrame> frames = stacktrace.getFrames();
		assertNotNull(frames);
		assertEquals( 4,  frames.size() );
		
		assertNotNull(frames.get(0));		
		assertNull(frames.get(0).getFileName());
		assertNull(frames.get(0).getLineNumber());
		assertEquals("/usr/lib/x86_64-linux-gnu/libstdc++.so.6.0.16", frames.get(0).getObjectName());
		
		assertNotNull(frames.get(3));		
		assertEquals( "main.cpp", frames.get(3).getFileName() );
		assertEquals( Integer.valueOf(24), frames.get(3).getLineNumber() );
		assertEquals("/home/jenkins/test-slave/workspace/valgrind-test/program1", frames.get(3).getObjectName());
		
		
		
	}
	
	@Test
	public void auxiliary() throws ParserConfigurationException, SAXException, IOException
	{	
		ValgrindReport report = parser.parse(new File("src/test/resources/org/jenkinsci/plugins/valgrind/parser/aux-data.xml"));
			
		assertNotNull( report );
		assertNotNull( report.getProcesses() );
		assertEquals( 1, report.getProcesses().size() );
		
		ValgrindProcess process = report.getProcesses().get(0);		
		assertEquals( "./program1", process.getExecutable() );

		assertEquals( 3, report.getErrorList().getErrorCount() );
		
		assertEquals( 2, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.UninitCondition));
		assertEquals( 1, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.Leak_DefinitelyLost));
		
		List<ValgrindError> uninitConditionErrors = report.getErrorList().getErrorsByKind(ValgrindErrorKind.UninitCondition);
		assertNotNull(uninitConditionErrors);
		assertEquals( 2, uninitConditionErrors.size() );
		
		ValgrindError error = uninitConditionErrors.get(0);
		assertNotNull(error);
		
		assertEquals( ValgrindErrorKind.UninitCondition, error.getKind() );
		assertEquals( "0x2" , error.getUniqueId() );
		assertEquals( "Conditional jump or move depends on uninitialised value(s)", error.getDescription() );		
		assertNull( error.getLeakedBlocks() );
		assertNull( error.getLeakedBytes() );
		
		ValgrindStacktrace stacktrace = error.getStacktrace();
		assertNotNull(stacktrace);
		
		List<ValgrindStacktraceFrame> frames = stacktrace.getFrames();
		assertNotNull(frames);
		assertEquals( 4,  frames.size() );
		
		assertNotNull(frames.get(0));		
		assertNull(frames.get(0).getFileName());
		assertNull(frames.get(0).getLineNumber());
		assertEquals("/usr/lib/x86_64-linux-gnu/libstdc++.so.6.0.16", frames.get(0).getObjectName());
		
		assertNotNull(frames.get(3));		
		assertEquals( "main.cpp", frames.get(3).getFileName() );
		assertEquals( Integer.valueOf(24), frames.get(3).getLineNumber() );
		assertEquals("/home/jenkins/test-slave/workspace/valgrind-test/program1", frames.get(3).getObjectName());	
		
		List<ValgrindAuxiliary> auxiliaryData = error.getAuxiliaryData();
		assertNotNull(auxiliaryData);
		assertEquals( 1, auxiliaryData.size() );
		
		ValgrindAuxiliary aux = auxiliaryData.get(0);
		assertNotNull(aux);
		
		assertEquals("Uninitialised value was created by a stack allocation", aux.getDescription());
		
		ValgrindStacktrace auxStacktrace = aux.getStacktrace();
		assertNotNull(auxStacktrace);
		assertNotNull(auxStacktrace.getFrames());
		assertEquals(1, auxStacktrace.getFrames().size());
		
		ValgrindStacktraceFrame auxFrame = auxStacktrace.getFrame(0);
		assertNotNull(auxFrame);
		assertEquals("main.cpp", auxFrame.getFileName());
		assertEquals(Integer.valueOf(10), auxFrame.getLineNumber());
	}


	@Test
	public void suppression() throws ParserConfigurationException, SAXException, IOException
	{
		ValgrindReport report = parser.parse(new File("src/test/resources/org/jenkinsci/plugins/valgrind/parser/aux-data.xml"));
		assertNotNull(report);

		ValgrindError error = report.findError("10418", "0x2");
		assertNotNull(error);

		final String expectedSuppression = hudson.Util.escape(
        "{\n" +
		"   <insert_a_suppression_name_here>\n" +
		"   Memcheck:Addr1\n" +
		"   fun:memcpy@@GLIBC_2.14\n" +
		"   fun:access_already_freed_memory_memcpy\n" +
		"   fun:main\n" +
		"}");

		assertEquals(expectedSuppression, error.getSuppression());
	}

	@Test
	public void auxiliary_noStacktrace() throws ParserConfigurationException, SAXException, IOException
	{	
		ValgrindReport report = parser.parse(new File("src/test/resources/org/jenkinsci/plugins/valgrind/parser/aux-noStacktrace.xml"));
			
		assertNotNull( report );
		assertNotNull( report.getProcesses() );
		assertEquals( 1, report.getProcesses().size() );
		
		ValgrindProcess process = report.getProcesses().get(0);		
		assertEquals( "./program1", process.getExecutable() );
		
		assertEquals( 3, report.getErrorList().getErrorCount() );
		
		assertEquals( 2, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.UninitCondition));
		assertEquals( 1, report.getErrorList().getErrorCountByKind(ValgrindErrorKind.Leak_DefinitelyLost));
		
		List<ValgrindError> uninitConditionErrors = report.getErrorList().getErrorsByKind(ValgrindErrorKind.UninitCondition);
		assertNotNull(uninitConditionErrors);
		assertEquals( 2, uninitConditionErrors.size() );
		
		ValgrindError error = uninitConditionErrors.get(1);
		assertNotNull(error);
		
		assertEquals( ValgrindErrorKind.UninitCondition, error.getKind() );
		assertEquals( "0x7" , error.getUniqueId() );
		assertEquals( "Conditional jump or move depends on uninitialised value(s)", error.getDescription() );
		assertNull( error.getLeakedBlocks() );
		assertNull( error.getLeakedBytes() );
		
		ValgrindStacktrace stacktrace = error.getStacktrace();
		assertNotNull(stacktrace);
		
		List<ValgrindStacktraceFrame> frames = stacktrace.getFrames();
		assertNotNull(frames);
		assertEquals( 4,  frames.size() );
		
		assertNotNull(frames.get(0));		
		assertNull(frames.get(0).getFileName());
		assertNull(frames.get(0).getLineNumber());
		assertEquals("/usr/lib/x86_64-linux-gnu/libstdc++.so.6.0.16", frames.get(0).getObjectName());
		
		assertNotNull(frames.get(3));		
		assertEquals( "main.cpp", frames.get(3).getFileName() );
		assertEquals( Integer.valueOf(24), frames.get(3).getLineNumber() );
		assertEquals("/home/jenkins/test-slave/workspace/valgrind-test/program1", frames.get(3).getObjectName());	
		
		List<ValgrindAuxiliary> auxiliaryData = error.getAuxiliaryData();
		assertNotNull(auxiliaryData);
		assertEquals( 2, auxiliaryData.size() );
		
		ValgrindAuxiliary aux1 = auxiliaryData.get(0);
		assertNotNull(aux1);
		
		assertEquals("Foobar", aux1.getDescription());
		assertNull(aux1.getStacktrace());
		
		ValgrindAuxiliary aux2 = auxiliaryData.get(1);
		assertNotNull(aux2);
		
		assertEquals("Uninitialised value was created by a stack allocation", aux2.getDescription());
		
		ValgrindStacktrace auxStacktrace = aux2.getStacktrace();
		assertNotNull(auxStacktrace);
		assertNotNull(auxStacktrace.getFrames());
		assertEquals(1, auxStacktrace.getFrames().size());
		
		ValgrindStacktraceFrame auxFrame = auxStacktrace.getFrame(0);
		assertNotNull(auxFrame);
		assertEquals("main.cpp", auxFrame.getFileName());
		assertEquals(Integer.valueOf(10), auxFrame.getLineNumber());				
	}
}
