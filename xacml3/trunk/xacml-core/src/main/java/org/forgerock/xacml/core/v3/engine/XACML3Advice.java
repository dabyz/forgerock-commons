
/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.xacml.core.v3.engine;

import com.sun.identity.entitlement.xacml3.core.Advice;
import com.sun.identity.entitlement.xacml3.core.AdviceExpression;
import com.sun.identity.entitlement.xacml3.core.AttributeAssignmentExpression;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.xacml.core.v3.model.DataAssignment;
import org.forgerock.xacml.core.v3.model.FunctionArgument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class XACML3Advice {

    private static Debug debug = Debug.getInstance("Xacml3");
    private String adviceID;
    private String appliesTo;
    private List<DataAssignment> advices;

    public XACML3Advice(AdviceExpression adv) {
        adviceID = adv.getAdviceId();
        appliesTo = adv.getAppliesTo().toString();
        advices = new ArrayList<DataAssignment>();

        for (AttributeAssignmentExpression ex : adv.getAttributeAssignmentExpression()) {
            advices.add(new DataAssignment(ex));
        }
    }

    public XACML3Advice() {
    }

    public String getAdviceID() {
        return adviceID;
    }
    public String getAppliesTo() {
        return appliesTo;
    }
    public List<DataAssignment> getAdvices() {
        return advices;
    }


    public XACML3Decision evaluate(XACMLEvalContext pip) {
        return null;
    }


    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();

        jo.put("classname",this.getClass().getName());
        jo.put("adviceID", adviceID);
        jo.put("appliesTo", appliesTo);

        JSONObject dassigns = new JSONObject();
        for (DataAssignment da : advices) {
            jo.append("advices",da.toJSONObject());
        }
        return jo;
    }

    public void init(JSONObject jo) throws JSONException {


        JSONObject dassigns = new JSONObject();
        for (DataAssignment da : advices) {
            jo.append("advices",da.toJSONObject());
        }

        adviceID = jo.optString("adviceID");
        appliesTo = jo.optString("appliesTo");

        advices = new ArrayList<DataAssignment>() ;

        JSONArray array = jo.getJSONArray("advices");
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = (JSONObject)array.get(i);
            advices.add((DataAssignment) FunctionArgument.getInstance(json));
        }
    }

    static public XACML3Advice getInstance(JSONObject jo)  {
        String className = jo.optString("classname");
        try {
            Class clazz = Class.forName(className);
            XACML3Advice farg = (XACML3Advice)clazz.newInstance();
            farg.init(jo);

            return farg;
        } catch (InstantiationException ex) {
            debug.error("FunctionArgument.getInstance", ex);
        } catch (IllegalAccessException ex) {
            debug.error("FunctionArgument.getInstance", ex);
        } catch (ClassNotFoundException ex) {
            debug.error("FunctionArgument.getInstance", ex);
        } catch (JSONException ex) {
            debug.error("FunctionArgument.getInstance", ex);
        }
        return null;
    }

    public Advice getAdvice(XACMLEvalContext pip) {
        Advice result = new Advice();

        result.setAdviceId(adviceID);

        for (DataAssignment das : advices) {
            result.getAttributeAssignment().add(das.getAssignment(pip)) ;
        }

        return result;
    }

    public AdviceExpression getAdviceExpression() {

        return null;
    }


}
