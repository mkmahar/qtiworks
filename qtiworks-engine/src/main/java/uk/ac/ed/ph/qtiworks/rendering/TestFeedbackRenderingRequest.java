/* Copyright (c) 2012-2013, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.qtiworks.rendering;

import uk.ac.ed.ph.jqtiplus.node.test.TestPart;
import uk.ac.ed.ph.jqtiplus.state.TestSessionState;

import javax.validation.constraints.NotNull;

/**
 * Request for rendering Test or {@link TestPart} feedback.
 *
 * @author David McKain
 */
public final class TestFeedbackRenderingRequest extends AbstractRenderingRequest implements TestRenderingRequest {

    /** Required {@link TestSessionState} to be rendered */
    @NotNull
    private TestSessionState testSessionState;

    private boolean testPartNavigationAllowed;

    //----------------------------------------------------

    @Override
    public TestSessionState getTestSessionState() {
        return testSessionState;
    }

    public void setTestSessionState(final TestSessionState testSessionState) {
        this.testSessionState = testSessionState;
    }


    @Override
    public boolean isTestPartNavigationAllowed() {
        return testPartNavigationAllowed;
    }

    public void setTestPartNavigationAllowed(final boolean testPartNavigationAllowed) {
        this.testPartNavigationAllowed = testPartNavigationAllowed;
    }


    @Override
    public boolean isEndTestPartAllowed() {
        return false;
    }

    @Override
    public boolean isReviewTestPartAllowed() {
        return false;
    }

    @Override
    public boolean isExitTestPartAllowed() {
        return true;
    }
}
