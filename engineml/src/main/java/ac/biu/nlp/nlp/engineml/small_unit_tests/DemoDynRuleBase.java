package ac.biu.nlp.nlp.engineml.small_unit_tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;

import eu.excitementproject.eop.common.datastructures.immutable.ImmutableSet;

import ac.biu.nlp.nlp.engineml.builtin_knowledge.KnowledgeResource;
import ac.biu.nlp.nlp.engineml.datastructures.LemmaAndPos;
import ac.biu.nlp.nlp.engineml.operations.rules.DynamicRuleBase;
import ac.biu.nlp.nlp.engineml.operations.rules.RuleBaseException;
import ac.biu.nlp.nlp.engineml.operations.rules.RuleWithConfidenceAndDescription;
import ac.biu.nlp.nlp.engineml.operations.rules.distsim.DistSimRuleBaseManager;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.engineml.utilities.TimeElapsedTracker;
import ac.biu.nlp.nlp.engineml.utilities.legacy.ExperimentLoggerNeutralizer;
import ac.biu.nlp.nlp.general.ExceptionUtil;
import ac.biu.nlp.nlp.general.configuration.ConfigurationException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationFile;
import ac.biu.nlp.nlp.general.configuration.ConfigurationFileDuplicateKeyException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationParams;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.Info;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.basic.BasicNode;
import ac.biu.nlp.nlp.representation.MiniparPartOfSpeech;
import ac.biu.nlp.nlp.representation.PartOfSpeech;

@Deprecated
public class DemoDynRuleBase
{
	public static void main(String[] args)
	{
		try
		{
			BasicConfigurator.configure();
			new ExperimentLoggerNeutralizer().neutralize();
			DemoDynRuleBase app = new DemoDynRuleBase(args);
			app.f();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	DemoDynRuleBase(String[] args)
	{
		this.args = args;
	}
	
	public void initRuleBase() throws ConfigurationFileDuplicateKeyException, ConfigurationException, ClassNotFoundException, SQLException, RuleBaseException, TeEngineMlException
	{
		ConfigurationFile confFile = new ConfigurationFile(args[0]);
		confFile.setExpandingEnvironmentVariables(true);
		KnowledgeResource resource = KnowledgeResource.ORIG_DIRT;
		ConfigurationParams params =  confFile.getModuleConfiguration(resource.getModuleName());
		manager = new DistSimRuleBaseManager(resource.getDisplayName(), params);
		manager.init();
		this.ruleBase = manager.getRuleBase();
		
//		MysqlDataSource dataSource = new MysqlDataSource();
//		dataSource.setServerName("qa-srv");
//		dataSource.setPort(3308);
//		dataSource.setUser("db_readonly");
//		connection = dataSource.getConnection();
//		
//		DistSimParameters originalDirtParameters =
//			new DistSimParameters("original_dirt.od_templates", "original_dirt.od_rules", LIMIT_DISTSIM_RULES, Constants.CACHES_SIZE.get("origdirtlhs"), Constants.CACHES_SIZE.get("origdirtrules"));
//		
//		ruleBase = new DistSimRuleBase(connection, originalDirtParameters, "origdirt");

		
	}
	public void cleanUpRuleBase() throws SQLException
	{
		manager.cleanUp();
		ruleBase = null;
	}
	
	public void f()
	{
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			initRuleBase();
			try
			{
				
				while(true)
				{
					TimeElapsedTracker tracker = new TimeElapsedTracker();
					String lemma = null;
					String posString = null;
					System.out.println("Enter lemma:");
					lemma = reader.readLine();
					System.out.println("Enter pos:");
					posString = reader.readLine();
					PartOfSpeech pos = new MiniparPartOfSpeech(posString);

					if ("bye".equalsIgnoreCase(lemma))
						break;
					Set<RuleWithConfidenceAndDescription<Info, BasicNode>> allRules = new LinkedHashSet<RuleWithConfidenceAndDescription<Info,BasicNode>>();
					
					tracker.start();
					ImmutableSet<BasicNode> lhss = ruleBase.getLeftHandSidesByLemmaAndPos(new LemmaAndPos(lemma, pos));
					tracker.end();
					for (BasicNode lhs : lhss)
					{
						tracker.start();
						ImmutableSet<RuleWithConfidenceAndDescription<Info, BasicNode>> rules =
							ruleBase.getRulesByLeftHandSide(lhs);
						tracker.end();
						for (RuleWithConfidenceAndDescription<Info, BasicNode> rule : rules)
						{
							allRules.add(rule);
						}
						
					}
					
					for (RuleWithConfidenceAndDescription<Info, BasicNode> rule : allRules)
					{
						System.out.println(
						rule.getDescription()+" with confidence: "+rule.getConfidence()
						);
					}
					System.out.println("Total: "+allRules.size()+" rules");
//					System.out.println("lhs query average: "+DistSimRuleBase.lhsTracker.getAverages());
//					System.out.println("rule query average: "+DistSimRuleBase.ruleTracker.getAverages());
					
					System.out.println("Accumulated time: "+tracker.getAccumulated());
					
//					DistSimRuleBase.lhsTracker = new TimeElapsedTracker();
//					DistSimRuleBase.ruleTracker = new TimeElapsedTracker();
				}
			}
			finally
			{
				cleanUpRuleBase();
			}
			
		}
		catch(Exception e)
		{
			ExceptionUtil.outputException(e, System.out);
		}
	}

	private DistSimRuleBaseManager manager;
	private DynamicRuleBase<Info, BasicNode> ruleBase;
	private String[] args;
}
