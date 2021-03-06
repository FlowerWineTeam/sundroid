package uk.co.sundroid.activity.info.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import uk.co.sundroid.AbstractFragment
import uk.co.sundroid.R
import uk.co.sundroid.util.theme.*

import kotlinx.android.synthetic.main.frag_info_glossary.*

class InfoGlossaryFragment : AbstractFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.frag_info_glossary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapOf(
                infGlossaryApsisDisclosure to infGlossaryApsisBody,
                infGlossaryAzimuthDisclosure to infGlossaryAzimuthBody,
                infGlossaryCivTwiDisclosure to infGlossaryCivTwiBody,
                infGlossaryNtcTwiDisclosure to infGlossaryNtcTwiBody,
                infGlossaryAstTwiDisclosure to infGlossaryAstTwiBody,
                infGlossarySolNoonDisclosure to infGlossarySolNoonBody,
                infGlossaryGoldenDisclosure to infGlossaryGoldenBody,
                infGlossarySolsticeDisclosure to infGlossarySolsticeBody,
                infGlossaryEquinoxDisclosure to infGlossaryEquinoxBody,
                infGlossarySolEclipseDisclosure to infGlossarySolEclipseBody,
                infGlossaryLunarEclipseDisclosure to infGlossaryLunarEclipseBody
        ).forEach { (d, body) -> d.setOnClickListener { v -> toggle(v, body)} }
    }

    private fun toggle(disclosure: View, body: View) {
        val view = view
        if (view == null || isDetached) {
            return
        }
        if (body.visibility == View.GONE) {
            body.visibility = View.VISIBLE
            (disclosure as TextView).setCompoundDrawablesWithIntrinsicBounds(0, 0, getDisclosureOpen(), 0)
        } else {
            body.visibility = View.GONE
            (disclosure as TextView).setCompoundDrawablesWithIntrinsicBounds(0, 0, getDisclosureClosed(), 0)
        }
    }

}
