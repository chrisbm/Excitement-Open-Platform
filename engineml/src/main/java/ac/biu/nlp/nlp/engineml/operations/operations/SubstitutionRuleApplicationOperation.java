package ac.biu.nlp.nlp.engineml.operations.operations;

import eu.excitementproject.eop.common.datastructures.BidirectionalMap;
import eu.excitementproject.eop.common.datastructures.SimpleBidirectionalMap;
import ac.biu.nlp.nlp.engineml.datastructures.DsUtils;
import ac.biu.nlp.nlp.engineml.datastructures.FromBidirectionalMapValueSetMap;
import ac.biu.nlp.nlp.engineml.operations.OperationException;
import ac.biu.nlp.nlp.engineml.operations.rules.Rule;
import ac.biu.nlp.nlp.engineml.utilities.InfoServices;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.Info;
import ac.biu.nlp.nlp.instruments.parse.tree.AbstractNode;
import ac.biu.nlp.nlp.instruments.parse.tree.AbstractNodeConstructor;
import ac.biu.nlp.nlp.instruments.parse.tree.TreeAndParentMap;

/**
 * This class performs the regular <B>rule application</B>.
 * It replaces the left-hand-side instance in the given tree by the instantiation
 * of the right hand side. 
 * 
 * @author Asher Stern
 * @since February  2011
 *
 * @param <TI>	type of the text node info
 * @param <TN>	type of the text node
 * @param <RI>	type of the rule node info
 * @param <RN>	type of the rule node
 */
public abstract class SubstitutionRuleApplicationOperation
	<TI extends Info, TN extends AbstractNode<TI, TN>, RI extends Info, RN extends AbstractNode<RI, RN>> 
	extends GenerationOperation<TI, TN>
{

	/**
	 * Constructor that defines the text tree, the rule, and other information
	 * required for applying the rule.
	 * 
	 * @param textTree The text tree
	 * @param hypothesisTree The hypothesis tree (not required, actually)
	 * @param rule The rule
	 * @param mapLhsToTree A one to one mapping from the rule's left-hand-side's
	 * nodes to nodes in the tree. Those tree nodes are actually a connected
	 * component in the tree that will be completely replaced by the rule's
	 * right-hand-side.
	 * @throws OperationException
	 */
	public SubstitutionRuleApplicationOperation(TreeAndParentMap<TI,TN> textTree, TreeAndParentMap<TI,TN> hypothesisTree,
			Rule<RI, RN> rule, BidirectionalMap<RN, TN> mapLhsToTree) throws OperationException
	{
		super(textTree, hypothesisTree);
		this.rule = rule;
		this.mapLhsToTree = mapLhsToTree;
	}

	@Override
	protected void generateTheTree() throws OperationException
	{
		bidiMapOrigToGenerated = new SimpleBidirectionalMap<TN, TN>();
		this.rootOfLhsInTree = mapLhsToTree.leftGet(rule.getLeftHandSide());
		if (null==rootOfLhsInTree) throw new OperationException("LHS root not mapped to any node in the original tree");
		try
		{
			generatedTree = copySubTree(this.textTree.getTree());
		}
		catch(TeEngineMlException e)
		{
			throw new OperationException("Rule application failed. See nested exception.",e);
		}
	}

	@Override
	protected void generateMapOriginalToGenerated() throws OperationException
	{
		mapOriginalToGenerated = new FromBidirectionalMapValueSetMap<TN, TN>(bidiMapOrigToGenerated);
	}
	
	
	protected TN copySubTree(TN subtree) throws TeEngineMlException
	{
		TN ret = null;
		if (rootOfLhsInTree == subtree)
		{
			rhsInstantiation = new RuleRhsInstantiation<TI, TN, RI, RN>(
					getNewInfoServices(), nodeConstructor, this.textTree.getTree(),
					rule, mapLhsToTree, subtree.getInfo());
			
			rhsInstantiation.generate();
			TN generatedInstance = rhsInstantiation.getGeneratedTree();
//			try {	RulesViewer.printTree(generatedInstance);	} 
//			catch (TreeStringGeneratorException e) {	e.printStackTrace();}
			DsUtils.BidiMapAddAll(bidiMapOrigToGenerated, rhsInstantiation.getMapOrigToGenerated());
			
//			// copy to the generatedInstance those children of rootOfLhsInTree that aren't matched in the rule 
//			for (TN child : rootOfLhsInTree.getChildren())
//			{
//				if (!bidiMapOrigToGenerated.leftContains(child) & !mapLhsToTree.rightContains(child))
//					generatedInstance.addChild(child);
//			}

//			try {
//				RulesViewer.printTree(generatedInstance);;
//			} catch (TreeStringGeneratorException e) {
//				e.printStackTrace();
//			}

			affectedNodes = rhsInstantiation.getAffectedNodes();
//			affectedNodes = new LinkedHashSet<TN>();
//			affectedNodes.addAll(AbstractNodeUtils.treeToSet(generatedInstance));

			ret = generatedInstance;
		}
		else
		{
			ret = nodeConstructor.newNode(subtree.getInfo());
			if (subtree.getChildren()!=null)
			{
				for (TN child : subtree.getChildren())
				{
					ret.addChild(copySubTree(child));
				}
			}
			bidiMapOrigToGenerated.put(subtree, ret);
		}
		
		return ret;
	}

	
	/**
	 * return a new implementation of {@link AbstractNodeConstructor} to match the generic types
	 * @return
	 */
	protected abstract AbstractNodeConstructor<TI, TN> getNewNodeConstructor();

	/**
	 * return a new implementation of InfoServices to match the generic types
	 * @return 
	 */
	protected abstract InfoServices<TI, RI> getNewInfoServices();


	protected AbstractNodeConstructor<TI, TN> nodeConstructor = getNewNodeConstructor();
	protected Rule<RI, RN> rule;
	protected BidirectionalMap<RN, TN> mapLhsToTree;
	
	protected RuleRhsInstantiation<TI, TN, RI, RN> rhsInstantiation;
	
	protected TN rootOfLhsInTree;
	protected BidirectionalMap<TN, TN> bidiMapOrigToGenerated;
	
	

}
